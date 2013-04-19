package Configuration;

#### default informations ####
our $name = 'Jens-Wolfhard Schicke-Uffmann';
our $email = 'drahflow@gmx.de';
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
  'ratbs' => {
    'project' => 'https://ticket.piraten-nds.de/projects/rat-bs',
    'issues' => 'https://ticket.piraten-nds.de/issues',
  }
);

our %redminePullFor = (
  $ownName => 1,
);

our %redmineUtilities = (
  'ratbs.Niedrig' => 0.8,
  'ratbs.Normal' => 2,
  'ratbs.Hoch' => 4,
  'ratbs.Sofort' => 10,
);

our %redmineLocations = (
  'ratbs' => ['slice_pirates'],
);

#### import-mail configuration ####

our @importMailInboxen = (
  "$ENV{HOME}/.icedove/evfwp6xx.default/Mail/pop.gmx.net/Inbox",
  "$ENV{HOME}/.icedove/evfwp6xx.default/Mail/pop.gmx.net/Inbox.sbd/Aktive-NDS",
  "$ENV{HOME}/.icedove/evfwp6xx.default/Mail/mail.ips.cs.tu-bs.de/Inbox",
  "$ENV{HOME}/.icedove/evfwp6xx.default/ImapMail/imap.tu-bs.de/INBOX-?\\d*\$",
  "$ENV{HOME}/.icedove/evfwp6xx.default/ImapMail/iserv.hvf-bs.net/INBOX-?\\d*\$",
  "$ENV{HOME}/.icedove/evfwp6xx.default/ImapMail/mail.piratenpartei-braunschweig-1.de/INBOX-?\\d*\$",
  "$ENV{HOME}/.icedove/evfwp6xx.default/ImapMail/mail.piratenpartei-braunschweig.de/INBOX-?\\d*\$",
  "$ENV{HOME}/.icedove/evfwp6xx.default/ImapMail/quuxlogic.com/INBOX-?\\d*\$",
  "$ENV{HOME}/.icedove/evfwp6xx.default/ImapMail/imap.quuxlogic.com/INBOX-?\\d*\$",
);

our $importMailOwnAddressRegEx = qr/(.*(drahflow|schicke).*|jens\@quuxLogic.com)/;
our $importMailTimeToReadOwnMail = 60;
our $importMailUtilityOfReadOwnMail = 10;
our $importMailTimeToReadMLMail = 10;

## program logic: ##

# provide getFooBar instead of $Configuration::fooBar, so the user can provide code instead of constant values
# if this is desired
sub AUTOLOAD {
  $AUTOLOAD =~ /^Configuration::get(.)(.*)/ or die "Configuration.pm AUTOLOAD expected getSomeValue()";
  my $name = lc($1).$2;
  
  my $syms = \%Configuration::;
  die "Configuration.pm AUTOLOAD asked to provide getter for nonexistent $name" unless exists $syms->{$name};
  *entry = $syms->{$name};

  return ${*entry{SCALAR}} if defined ${*entry{SCALAR}};
  return @{*entry{ARRAY}} if defined *entry{ARRAY};
  return %{*entry{HASH}} if defined *entry{HASH};
};

1;
