CREATE TABLE `bases` (
  `id` integer not null auto_increment,
	`active` longtext not null,
	`arbeiter` integer not null,
	`autogtuacts` longtext not null,
	`bebauung` longtext not null,
	`bewohner` integer not null,
	`cargo` longtext not null,
	`core` integer not null,
	`coreactive` integer not null,
	`e` integer not null,
	`height` integer not null,
	`isfeeding` boolean not null,
	`isloading` boolean not null,
	`maxcargo` bigint not null,
	`maxe` integer not null,
	`maxtiles` integer not null,
	`name` varchar(255) not null,
	`size` integer not null,
	`spawnableress` longtext,
	`spawnressavailable` longtext,
	`system` integer not null,
	`terrain` longtext not null,
	`version` integer not null,
	`width` integer not null,
	`x` integer not null,
	`y` integer not null,
	academy_id integer,
	forschungszentrum_id integer,
	`klasse` integer not null,
	`owner` integer not null,
	werft_id integer,
  primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
