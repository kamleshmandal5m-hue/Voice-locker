-- Shared Color King demo picks: up to four votes per user per 10-second period.
-- Result is one of Red, Green, Violet only; no mixed colors are returned.

create table if not exists public.ck_color_picks (
  id bigserial primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  period bigint not null,
  color text not null check (color in ('Red','Green','Violet')),
  amount integer not null check (amount > 0),
  created_at timestamptz not null default now()
);

alter table public.ck_color_picks
  drop constraint if exists ck_color_picks_user_id_period_key;
alter table public.ck_color_picks enable row level security;
revoke all on public.ck_color_picks from anon, authenticated;

create or replace function public.ck_submit_color_pick(
  p_period bigint,
  p_color text,
  p_amount integer
) returns boolean
language plpgsql
security definer
set search_path = public, auth
as $function$
declare
  vote_count integer;
begin
  if auth.uid() is null then
    raise exception 'Login required';
  end if;
  if p_period is null or p_color not in ('Red','Green','Violet')
     or p_amount is null or p_amount <= 0 then
    raise exception 'Invalid color selection';
  end if;

  -- Serialize submissions for this account and round to prevent exceeding four votes.
  perform pg_advisory_xact_lock(hashtextextended(auth.uid()::text || ':' || p_period::text, 0));
  select count(*) into vote_count
  from public.ck_color_picks
  where user_id = auth.uid() and period = p_period;

  if vote_count >= 4 then
    raise exception 'Maximum four votes allowed in one round';
  end if;

  insert into public.ck_color_picks(user_id, period, color, amount)
  values (auth.uid(), p_period, p_color, p_amount);

  return true;
end;
$function$;

create or replace function public.ck_get_auto_color_result(p_period bigint)
returns text
language plpgsql
security definer
set search_path = public
as $function$
declare
  red_count bigint;
  green_count bigint;
  low_color text;
begin
  if p_period is null then return null; end if;

  if not exists (select 1 from public.ck_color_picks where period = p_period) then
    return case when random() < 0.5 then 'Red' else 'Green' end;
  end if;

  select count(distinct user_id) into red_count
  from public.ck_color_picks where period = p_period and color = 'Red';

  select count(distinct user_id) into green_count
  from public.ck_color_picks where period = p_period and color = 'Green';

  -- Violet-only selections do not count as a Red/Green tie.
  if red_count = 0 and green_count = 0 then
    return case when random() < 0.5 then 'Red' else 'Green' end;
  end if;

  if red_count = green_count then
    -- When Red and Green counts are tied, choose one base color with Violet.
    low_color := case when random() < 0.5 then 'Red' else 'Green' end;
    return low_color || '+Violet';
  elsif red_count > green_count then
    low_color := 'Green';
  else
    low_color := 'Red';
  end if;

  -- Violet is added only when Red and Green counts are exactly equal.
  -- Any unequal count returns only the less-selected color.
  return low_color;
end;
$function$;

revoke all on function public.ck_submit_color_pick(bigint,text,integer) from public, anon;
grant execute on function public.ck_submit_color_pick(bigint,text,integer) to authenticated;
revoke all on function public.ck_get_auto_color_result(bigint) from public, anon;
grant execute on function public.ck_get_auto_color_result(bigint) to authenticated;

notify pgrst, 'reload schema';
