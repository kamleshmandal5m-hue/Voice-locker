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
  least_count bigint;
  least_colors text[];
begin
  if p_period is null then
    return null;
  end if;

  -- No votes: choose randomly between Red and Green.
  if not exists (select 1 from public.ck_color_picks where period = p_period) then
    return case when random() < 0.5 then 'Red' else 'Green' end;
  end if;

  -- If only one color has any votes, return one of the other two colors.
  if (select count(distinct color) from public.ck_color_picks where period = p_period) = 1 then
    select array_agg(c.color order by c.color) into least_colors
    from unnest(array['Red','Green','Violet']::text[]) as c(color)
    where c.color not in (
      select distinct color from public.ck_color_picks where period = p_period
    );
    return least_colors[1 + floor(random() * cardinality(least_colors))::integer];
  end if;

  -- With two or three selected colors, compare unique people per color.
  with color_counts as (
    select color, count(distinct user_id) as n
    from public.ck_color_picks
    where period = p_period
    group by color
  )
  select min(n) into least_count from color_counts;

  with color_counts as (
    select color, count(distinct user_id) as n
    from public.ck_color_picks
    where period = p_period
    group by color
  )
  select array_agg(color order by color) into least_colors
  from color_counts
  where n = least_count;

  -- Ties are resolved randomly among the least-selected colors.
  return least_colors[1 + floor(random() * cardinality(least_colors))::integer];
end;
$function$;

revoke all on function public.ck_submit_color_pick(bigint,text,integer) from public, anon;
grant execute on function public.ck_submit_color_pick(bigint,text,integer) to authenticated;
revoke all on function public.ck_get_auto_color_result(bigint) from public, anon;
grant execute on function public.ck_get_auto_color_result(bigint) to authenticated;

notify pgrst, 'reload schema';
