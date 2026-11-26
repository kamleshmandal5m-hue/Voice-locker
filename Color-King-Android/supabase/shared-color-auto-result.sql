-- Shared Color King demo picks and automatic least-selected color result.
-- Run this file once in Supabase SQL Editor before installing the updated APK.

create table if not exists public.ck_color_picks (
  id bigserial primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  period bigint not null,
  color text not null check (color in ('Red','Green','Violet')),
  amount integer not null check (amount > 0),
  created_at timestamptz not null default now(),
  unique (user_id, period)
);

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
begin
  if auth.uid() is null then
    raise exception 'Login required';
  end if;
  if p_period is null or p_color not in ('Red','Green','Violet')
     or p_amount is null or p_amount <= 0 then
    raise exception 'Invalid color selection';
  end if;

  insert into public.ck_color_picks(user_id, period, color, amount)
  values (auth.uid(), p_period, p_color, p_amount)
  on conflict (user_id, period) do update
    set color = excluded.color, amount = excluded.amount, created_at = now();

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

  -- Include every available color, even when nobody selected it (count = 0).
  with color_counts as (
    select c.color, count(p.id) as n
    from (values ('Red'), ('Green'), ('Violet')) as c(color)
    left join public.ck_color_picks p
      on p.period = p_period and p.color = c.color
    group by c.color
  )
  select min(n) into least_count from color_counts;

  with color_counts as (
    select c.color, count(p.id) as n
    from (values ('Red'), ('Green'), ('Violet')) as c(color)
    left join public.ck_color_picks p
      on p.period = p_period and p.color = c.color
    group by c.color
  )
  select array_agg(color order by color) into least_colors
  from color_counts
  where n = least_count;

  if cardinality(least_colors) = 1 then
    return least_colors[1];
  elsif least_colors @> array['Red','Green']::text[]
     and cardinality(least_colors) = 2 then
    return 'Yellow';
  elsif least_colors @> array['Red','Violet']::text[]
     and cardinality(least_colors) = 2 then
    return 'Magenta';
  elsif least_colors @> array['Green','Violet']::text[]
     and cardinality(least_colors) = 2 then
    return 'Cyan';
  else
    return 'White';
  end if;
end;
$function$;

revoke all on function public.ck_submit_color_pick(bigint,text,integer) from public, anon;
grant execute on function public.ck_submit_color_pick(bigint,text,integer) to authenticated;
revoke all on function public.ck_get_auto_color_result(bigint) from public, anon;
grant execute on function public.ck_get_auto_color_result(bigint) to authenticated;

notify pgrst, 'reload schema';
