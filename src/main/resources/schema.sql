create table travelers (
  id bigint primary key,
  username varchar(50) not null,
  display_name varchar(100) not null,
  password_hash varchar(100) not null,
  role varchar(20) not null
);
create table vouchers (
  id bigint primary key,
  traveler_id bigint not null,
  destination varchar(100) not null,
  purpose varchar(200),
  status varchar(20) not null,
  start_date date not null,
  end_date date not null,
  daily_rate_cents bigint not null
);
create table line_items (
  id bigint primary key,
  voucher_id bigint not null,
  category varchar(30) not null,
  amount_cents bigint not null
);
create index idx_vouchers_traveler_id on vouchers (traveler_id);
create index idx_line_items_voucher_id on line_items (voucher_id);
