# Ktor ORM demo
ktorm v.s. exposed

# Local Test Database
* mac: `sh ./docker/start.sh`
* windows: `bash ./docker/start.sh`

or in your other local database:
```sql
create sequence test_memo_seq;
grant select, usage on sequence test_memo_seq to #super#;

create table public.test_memo (
  id int4 not null default nextval('public.test_memo_seq' :: regclass),
  memo varchar(50) not null,
  create_time timestamp not null default (now() at time zone 'utc'),
  update_time timestamp null,
  constraint test_memo_pkey primary key (id)
);

grant select on table public.test_memo to #username#;
grant select,insert,delete,update on table public.test_memo to #super#;

```