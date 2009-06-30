CREATE TABLE `buildings` (
  `id` int(11) NOT NULL auto_increment,
  `name` varchar(50) NOT NULL default 'Noname',
  `picture` varchar(60) NOT NULL default '',
  `buildcosts` varchar(120) NOT NULL default '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,',
  `consumes` varchar(120) NOT NULL default '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,',
  `produces` varchar(120) NOT NULL default '0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,',
  `arbeiter` int(11) NOT NULL default '0',
  `ever` int(11) NOT NULL default '0',
  `eprodu` int(11) NOT NULL default '0',
  `bewohner` int(11) NOT NULL default '0',
  `techreq` int(11) NOT NULL default '0',
  `eps` int(11) NOT NULL default '0',
  `perplanet` tinyint(3) unsigned NOT NULL default '0',
  `perowner` tinyint(3) unsigned NOT NULL default '0',
  `category` tinyint(3) unsigned NOT NULL default '4',
  `ucomplex` tinyint(3) unsigned NOT NULL default '0',
  `deakable` tinyint(1) unsigned NOT NULL default '1',
  `module` varchar(60) NOT NULL default 'net.driftingsouls.ds2.server.bases.DefaultBuilding',
  `race` tinyint(3) NOT NULL default '0'
  PRIMARY KEY  (`id`),
  KEY `category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
