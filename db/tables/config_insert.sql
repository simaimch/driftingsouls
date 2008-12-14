INSERT INTO config (
	`name` ,
	`value` ,
	`description` ,
	`version` 
)
VALUES (
	'ticks', '2774', 'Der aktuelle Tick', '0'
), (
	'disablelogin', '', 'Begruendung, weshalb der Login abgeschalten ist (Leeres Feld == Login moeglich)', '0'
), (
	'disableregister', '', 'Begruendung, weshalb man sich nicht registrieren kann (Leeres Feld == registrieren moeglich)', '0'
), (
	'keys', '*', 'Schluessel mit denen man sich registrieren kann, wenn der Wert * ist braucht man keinen Schluessel zum registrieren', '0'
), (
	'foodpooldegeneration', '0', 'Prozent des Pools, die pro Tick verfaulen', '0'
), (
	'bvsizemodifier', 1, 'Faktor fuer die Groesse bei der Battle Value Formel', '0'
), (
	'bvdockmodifier', 1, 'Faktor fuer die Dockanzahl bei der Battle Value Formel', '0'
), (
	'endtiemodifier', 5, 'Faktor fuer die Anzahl der Schiffe, die man mehr haben muss, um einen Kampf unentschieden zu beenden', '0'
);
