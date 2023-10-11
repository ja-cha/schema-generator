------------------------------------- preview schema --------------------------------------

---------------------------------------- node ----------------------------------------

drop table if exists node cascade;

create table node (
  id varchar(64) not null primary key,
  node_type varchar(64) not null,
  created_at timestamp not null default current_timestamp,
  modified_at timestamp not null default current_timestamp,
  properties jsonb not null default '{}'::json,
  published boolean not null default false,
  dirty boolean not null default false,
  deleted boolean not null default false,
  checkpoint_id bigint not null default 1000,
  last_published_at timestamp,
  filters text[]
);


create index node_ix01 on node (node_type);
create index node_ix02 on node using gin (properties);

---------------------------------------- node_rev ----------------------------------------

drop table if exists node_rev cascade;

create table node_rev (
  id varchar(64) not null primary key,
  node_id varchar(64) not null references node (id),
  created_at timestamp not null default current_timestamp,
  properties jsonb not null,
  published boolean  not null,
  dirty boolean  not null,
  checkpoint_id bigint not null,
  deleted boolean not null default false,
  node_type varchar(64) not null,
  last_published_at timestamp,
  filters text[]
);

create index node_rev_ix01 on node_rev (node_id);
create index node_rev_ix02 on node_rev using gin (properties);
create unique index node_rev_ix03 on node_rev (node_id,checkpoint_id);

---------------------------------------- edge ----------------------------------------

drop table if exists edge cascade;

create table edge (
  id varchar(64) not null primary key,
  edge_type varchar(64) not null,
  from_node_id varchar(64) not null references node (id),
  to_node_id varchar(64) not null references node (id),
  created_at timestamp not null default current_timestamp,
  modified_at timestamp not null default current_timestamp,
  properties jsonb not null default '{}'::json,
  published boolean not null default false,
  dirty boolean not null default false,
  deleted boolean not null default false,
  checkpoint_id bigint not null default 1000,
  last_published_at timestamp,
  filters text[]
);

create index edge_ix01 on edge (edge_type);
create index edge_ix02 on edge using gin (properties);
create index edge_ix03 on edge (from_node_id);
create index edge_ix04 on edge (to_node_id);

---------------------------------------- edge_rev ----------------------------------------

drop table if exists edge_rev cascade;

create table edge_rev (
  id varchar(64) not null primary key,
  edge_id varchar(64) not null references edge (id),
  edge_type varchar(64) not null,
  from_node_id varchar(64) not null references node (id),
  to_node_id varchar(64) not null references node (id),
  created_at timestamp not null default current_timestamp,
  properties jsonb not null default '{}'::json,
  published boolean not null default false,
  dirty boolean not null default false,
  checkpoint_id bigint not null,
  deleted boolean not null default false,
  last_published_at timestamp,
  filters text[]
);

create index edge_rev_ix01 on edge_rev (edge_id);
create index edge_rev_ix02 on edge_rev using gin (properties);
create unique index edge_rev_ix03 on edge_rev (edge_id,checkpoint_id);


--------------------------------------- live schema ---------------------------------------

------------------------------------------ node -------------------------------------------

drop table if exists live.node cascade;

create table live.node (
  id varchar(64) not null primary key,
  node_type varchar(64) not null,
  created_at timestamp not null default current_timestamp,
  modified_at timestamp not null default current_timestamp,
  properties jsonb not null default '{}'::json,
  published boolean not null default false,
  dirty boolean not null default false,
  deleted boolean not null default false,
  checkpoint_id bigint not null default 1000,
  last_published_at timestamp,
  filters text[]
);

create index node_ix01 on live.node (node_type);
create index node_ix02 on live.node using gin (properties);

------------------------------------------ edge -------------------------------------------

drop table if exists live.edge cascade;

create table live.edge (
  id varchar(64) not null primary key,
  edge_type varchar(64) not null,
  from_node_id varchar(64) not null references live.node (id),
  to_node_id varchar(64) not null references live.node (id),
  created_at timestamp not null default current_timestamp,
  modified_at timestamp not null default current_timestamp,
  properties jsonb not null default '{}'::json,
  published boolean not null default false,
  deleted boolean not null default false,
  dirty boolean not null default false,
  checkpoint_id bigint not null default 1000,
  last_published_at timestamp,
  filters text[]
);

create index edge_ix01 on live.edge (edge_type);
create index edge_ix02 on live.edge using gin (properties);

---------------------------------------- checkpoint_seq -------------------------------------------

drop sequence if exists checkpoint_seq;

create sequence checkpoint_seq start 10001;

------------------------------------------- checkpoint --------------------------------------------

drop table if exists checkpoint;

create table checkpoint (
  id bigint not null primary key,
  created_at timestamp not null default current_timestamp,
  deleted boolean not null default false
);

---------------------------------------- node (checkpoint) ----------------------------------------

drop function if exists node(bigint);

create function node(bigint) returns table (id varchar,properties jsonb,modified_at timestamp,checkpoint_id bigint,published boolean,deleted boolean,dirty boolean,node_type varchar) AS $$
select x.* 
from (
  select n.id, 
         n.properties, 
         n.modified_at, 
         n.checkpoint_id,
         n.published,
         n.deleted,
         n.dirty,
         n.node_type
  from   node n  
  union 
  select nv.node_id, 
         nv.properties,  
         nv.created_at, 
         nv.checkpoint_id,
         nv.published,
         nv.deleted,
         nv.dirty,
         nv.node_type
  from node_rev nv
) as x 
join
(
  select id,
         max(checkpoint_id) as max_checkpoint_id  
  from (
    select id, 
           checkpoint_id 
    from   node n
    union 
    select node_id, 
           nv.checkpoint_id 
    from   node_rev nv
  ) as y 
  
  where checkpoint_id <= $1 group by id
) z 
on x.id = z.id and x.checkpoint_id=z.max_checkpoint_id
$$
LANGUAGE SQL
STABLE
;

---------------------------------------- edge (checkpoint) ----------------------------------------

drop function if exists edge  (bigint);

create function edge(bigint) returns table (id varchar,properties jsonb,modified_at timestamp,checkpoint_id bigint,published boolean,deleted boolean,dirty boolean,edge_type varchar,from_node_id varchar,to_node_id varchar)  AS $$
select x.* 
from (
  select e.id, 
         e.properties, 
         e.modified_at, 
         e.checkpoint_id,
         e.published,
         e.deleted,
         e.dirty,
         e.edge_type,
         e.from_node_id,
         e.to_node_id
  from   edge e
  union 
  select ev.edge_id, 
         ev.properties,  
         ev.created_at, 
         ev.checkpoint_id,
         ev.published,
         ev.deleted,
         ev.dirty,
         ev.edge_type,
         ev.from_node_id,
         ev.to_node_id
  from edge_rev ev
) as x 
join
(
  select id,
         max(checkpoint_id) as max_checkpoint_id  
  from (
    select e.id, 
           e.checkpoint_id 
    from   edge e
    union 
    select ev.edge_id, 
           ev.checkpoint_id 
    from   edge_rev ev
  ) as y 
  
  where y.checkpoint_id <= $1 group by id
) z 
on x.id = z.id and x.checkpoint_id=z.max_checkpoint_id
$$
LANGUAGE SQL
STABLE
;
