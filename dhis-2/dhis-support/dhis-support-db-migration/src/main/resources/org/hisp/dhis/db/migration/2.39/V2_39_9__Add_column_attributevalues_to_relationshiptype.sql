alter table relationshiptype add column if not exists attributevalues jsonb default '{}'::jsonb;
alter table attribute add column if not exists relationshipTypeAttribute boolean not null default false;