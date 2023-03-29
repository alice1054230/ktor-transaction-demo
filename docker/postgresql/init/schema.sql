create sequence test_memo_seq;

create table public.test_memo (
  id int4 not null default nextval('public.test_memo_seq' :: regclass),
  memo varchar(50) not null,
  create_time timestamp not null default (now() at time zone 'utc'),
  update_time timestamp null,
  constraint test_memo_pkey primary key (id)
);
