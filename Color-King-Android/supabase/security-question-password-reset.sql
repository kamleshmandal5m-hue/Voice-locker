-- Color King account security for email-auth accounts with mobile-number aliases.
-- Run this updated script in Supabase SQL Editor after deploying the matching app build.
create extension if not exists pgcrypto with schema extensions;

create table if not exists public.ck_account_security (
  user_id uuid primary key references auth.users(id) on delete cascade,
  question text not null,
  answer_hash text not null,
  created_at timestamptz not null default now()
);
alter table public.ck_account_security enable row level security;
revoke all on public.ck_account_security from anon, authenticated;

-- Upgrade older installations where the table already existed without phone.
alter table public.ck_account_security
  add column if not exists phone text;

update public.ck_account_security s
set phone = u.raw_user_meta_data->>'phone'
from auth.users u
where u.id = s.user_id
  and s.phone is null
  and nullif(u.raw_user_meta_data->>'phone','') is not null;

create unique index if not exists ck_account_security_phone_uidx
  on public.ck_account_security(phone)
  where phone is not null;

create table if not exists public.ck_password_reset_attempts (
  id bigserial primary key,
  phone text not null,
  attempted_at timestamptz not null default now()
);
revoke all on public.ck_password_reset_attempts from anon, authenticated;
alter table public.ck_password_reset_attempts enable row level security;

create or replace function public.ck_set_security_question(p_question text, p_answer text)
returns boolean language plpgsql security definer
set search_path = public, extensions, auth
as $1
  if auth.uid() is null or length(trim(coalesce(p_question,''))) < 3
     or length(trim(coalesce(p_answer,''))) < 2 then
    raise exception 'A security question and answer are required';
  end if;
  insert into public.ck_account_security(user_id, phone, question, answer_hash)
  select auth.uid(), u.raw_user_meta_data->>'phone', trim(p_question),
         extensions.crypt(lower(trim(p_answer)), extensions.gen_salt('bf', 12))
  from auth.users u where u.id=auth.uid()
  on conflict(user_id) do update
    set phone=excluded.phone, question=excluded.question,
        answer_hash=excluded.answer_hash, created_at=now();
  return true;
end; $$;

-- Remove the earlier versions that required users to type the question again.
drop function if exists public.ck_verify_security_answer(text,text,text);
drop function if exists public.ck_reset_password_by_security(text,text,text,text);

create or replace function public.ck_is_registered_phone(p_phone text)
returns boolean language sql security definer
set search_path = public, auth
as $$
  select exists (
    select 1 from auth.users u
    join public.ck_account_security s on s.user_id=u.id
    where s.phone = p_phone
  );
$$;

create or replace function public.ck_verify_security_answer(
  p_phone text, p_answer text
) returns boolean language plpgsql security definer
set search_path = public, extensions, auth
as $$
declare u_id uuid; stored_hash text; tries integer;
begin
  if p_phone is null or p_answer is null then return false; end if;
  select count(*) into tries from public.ck_password_reset_attempts
    where phone=p_phone and attempted_at > now()-interval '1 hour';
  if tries >= 5 then return false; end if;
  insert into public.ck_password_reset_attempts(phone) values(p_phone);
  select id into u_id from auth.users
    where email=('ck' || p_phone || '@users.colorking.invalid') limit 1;
  if u_id is null then return false; end if;
  select answer_hash into stored_hash
    from public.ck_account_security where user_id=u_id;
  if stored_hash is null or
     stored_hash <> extensions.crypt(lower(trim(coalesce(p_answer,''))), stored_hash) then
    return false;
  end if;
  return true;
end; $$;

create or replace function public.ck_reset_password_by_security(
  p_phone text, p_answer text, p_new_password text
) returns boolean language plpgsql security definer
set search_path = public, extensions, auth
as $$
declare u_id uuid; stored_hash text; tries integer;
begin
  if p_phone is null or p_answer is null or length(coalesce(p_new_password,'')) < 8 then return false; end if;
  select count(*) into tries from public.ck_password_reset_attempts
    where phone=p_phone and attempted_at > now()-interval '1 hour';
  if tries >= 5 then return false; end if;
  insert into public.ck_password_reset_attempts(phone) values(p_phone);
  select id into u_id from auth.users
    where email=('ck' || p_phone || '@users.colorking.invalid') limit 1;
  if u_id is null then return false; end if;
  select answer_hash into stored_hash
    from public.ck_account_security where user_id=u_id;
  if stored_hash is null or
     stored_hash <> extensions.crypt(lower(trim(coalesce(p_answer,''))), stored_hash) then
    return false;
  end if;
  update auth.users
    set encrypted_password=extensions.crypt(p_new_password, extensions.gen_salt('bf')), updated_at=now()
    where id=u_id;
  delete from public.ck_password_reset_attempts
    where phone=p_phone and attempted_at < now()-interval '1 hour';
  return true;
end; $$;

revoke all on function public.ck_set_security_question(text,text) from public, anon;
grant execute on function public.ck_set_security_question(text,text) to authenticated;
revoke all on function public.ck_reset_password_by_security(text,text,text) from public, authenticated;
grant execute on function public.ck_reset_password_by_security(text,text,text) to anon;
revoke all on function public.ck_is_registered_phone(text) from public, authenticated;
grant execute on function public.ck_is_registered_phone(text) to anon;
revoke all on function public.ck_verify_security_answer(text,text) from public, authenticated;
grant execute on function public.ck_verify_security_answer(text,text) to anon;

-- Refresh PostgREST function metadata after applying this migration.
NOTIFY pgrst, 'reload schema';
