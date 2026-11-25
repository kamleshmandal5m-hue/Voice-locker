-- Color King account security. Run once in Supabase SQL Editor.
create extension if not exists pgcrypto with schema extensions;

create table if not exists public.ck_account_security (
  user_id uuid primary key references auth.users(id) on delete cascade,
  question text not null,
  answer_hash text not null,
  created_at timestamptz not null default now()
);
alter table public.ck_account_security enable row level security;
revoke all on public.ck_account_security from anon, authenticated;

create table if not exists public.ck_password_reset_attempts (
  id bigserial primary key,
  phone text not null,
  attempted_at timestamptz not null default now()
);
revoke all on public.ck_password_reset_attempts from anon, authenticated;

create or replace function public.ck_set_security_question(p_question text, p_answer text)
returns boolean language plpgsql security definer
set search_path = public, extensions, auth
as $$
begin
  if auth.uid() is null or length(trim(coalesce(p_question,''))) < 3
     or length(trim(coalesce(p_answer,''))) < 2 then
    raise exception 'A security question and answer are required';
  end if;
  insert into public.ck_account_security(user_id, question, answer_hash)
  values(auth.uid(), trim(p_question), extensions.crypt(lower(trim(p_answer)), extensions.gen_salt('bf', 12)))
  on conflict(user_id) do update set question=excluded.question, answer_hash=excluded.answer_hash, created_at=now();
  return true;
end; $$;

create or replace function public.ck_reset_password_by_security(
  p_phone text, p_question text, p_answer text, p_new_password text
) returns boolean language plpgsql security definer
set search_path = public, extensions, auth
as $$
declare u_id uuid; stored_q text; stored_hash text; tries integer;
begin
  if length(coalesce(p_new_password,'')) < 8 then return false; end if;
  select count(*) into tries from public.ck_password_reset_attempts
    where phone=p_phone and attempted_at > now()-interval '1 hour';
  if tries >= 5 then return false; end if;
  insert into public.ck_password_reset_attempts(phone) values(p_phone);
  select id into u_id from auth.users where phone=p_phone limit 1;
  if u_id is null then return false; end if;
  select question, answer_hash into stored_q, stored_hash
    from public.ck_account_security where user_id=u_id;
  if stored_hash is null or stored_q <> trim(coalesce(p_question,'')) or
     stored_hash <> extensions.crypt(lower(trim(coalesce(p_answer,''))), stored_hash) then
    return false;
  end if;
  update auth.users set encrypted_password=extensions.crypt(p_new_password, extensions.gen_salt('bf')), updated_at=now()
    where id=u_id;
  delete from public.ck_password_reset_attempts
    where phone=p_phone and attempted_at < now()-interval '1 hour';
  return true;
end; $$;

revoke all on function public.ck_set_security_question(text,text) from public, anon;
grant execute on function public.ck_set_security_question(text,text) to authenticated;
revoke all on function public.ck_reset_password_by_security(text,text,text,text) from public, authenticated;
grant execute on function public.ck_reset_password_by_security(text,text,text,text) to anon;
