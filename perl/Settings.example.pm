package Settings;

#### default informations ####
our $name = '<Your Name>';
our $email = '<Your.Email@Address.com';
our $browser = "firefox";
our $windowManager = "wmii";

#### redmine configuration ####

# this is just for the paranoid: hardcode the valid certificates, so rouge CAs don't concern us
our %redmineGoodCerts = (
  'e0afa607ec8264ab8134043672aa7c2bb617d4160cec998cbf9597ce759b1f9b4fe84c811ac6f2b5fc9590142ee6fe8812a936ce506d5d305d979ec275f15295' => 1,
  '4edf1edc86fb5a2d25a3e4891a14f7981529ad5052185a7d81d7700d847c2a1d74cee8c8731c1c335b85b8b8dda8b81ad03e1d1754271cbcff647c5b110f9907' => 1,
  '0b870e54f7bc1405107bfc9e45148c64860630f2f5dfe91d6c0f620e88e9ba3c729a94e07229238abb002052aa4a38ec4faf9a79b4b5062bedc966798d6d7312' => 1,
  'bd5041200fb99b8bba96ca23091c4c14ccd89e42a27b85543eab193736674ff75d7a922a2eccb7a93ac4907658b25eee26f32d95eca18103bf65b57c0c5f6911' => 1,
  '1e9346f8fdc9e901e4846fdfc7defaba5a514b2422d60433fa9c63c36b729db8c549c3b1c1c6696528d2727045585e6d91ec7fc86ebd64f764295e8f62d199fb' => 1,
  '25fa0d2f2ef252962e2c26f1a03ac9a630a2d782bbb3f42b6c15cedcd6e8d1190ed4d0c1f781808e553e20b8b146c3871511b9aae2b50d3df3b55b41bc0d8345' => 1,
);

our %redmineNames = (
  'exampleProject' => {
    'project' => 'https://ticket.example.com/projects/exampleproject',
    'issues' => 'https://ticket.example.com/issues',
  },
);

our %redminePullFor = (
  $ownName => 1,
);

our %redmineUtilities = (
  'exampleProject.Niedrig' => 0.8,
  'exampleProject.Normal' => 2,
  'exampleProject.Hoch' => 4,
  'exampleProject.Sofort' => 10,
);

our %redmineLocations = (
  'exampleProject' => ['slice_work'],
);

#### import-mail configuration ####

our @importMailInboxen = (
  "$ENV{HOME}/Inbox.mbox",
);

our $importMailOwnAddressRegEx = qr/(.*(drahflow|schicke).*|jens\@quuxLogic.com)/;
our $importMailTimeToReadOwnMail = 60;
our $importMailUtilityOfReadOwnMail = 10;
our $importMailTimeToReadMLMail = 10;

## program logic: ##

# provide getFooBar instead of $Settings::fooBar, so the user can provide code instead of constant values
# if this is desired
sub AUTOLOAD {
  $AUTOLOAD =~ /^Settings::get(.)(.*)/ or die "Settings.pm AUTOLOAD expected getSomeValue()";
  my $name = lc($1).$2;
  
  my $syms = \%Settings::;
  die "Settings.pm AUTOLOAD asked to provide getter for nonexistent $name" unless exists $syms->{$name};
  *entry = $syms->{$name};

  return ${*entry{SCALAR}} if defined ${*entry{SCALAR}};
  return @{*entry{ARRAY}} if defined *entry{ARRAY};
  return %{*entry{HASH}} if defined *entry{HASH};
};

1;
