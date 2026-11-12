[out:json][timeout:180];
rel["boundary"="administrative"]["admin_level"="4"]["ISO3166-2"~"^VN-"];
map_to_area->.provs;
foreach.provs->.p(
  .p out tags;
  nwr["amenity"="cinema"](area.p);
  out center tags;
);
