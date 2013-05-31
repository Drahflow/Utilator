package Secrets;

use Digest::MD5 qw(md5_hex);
use Global qw(space_by_name);
use Digest::MD5 qw(md5);

# md5sum of email address => estimated utility of reading a mail from that person
my %importMailExtraUtilBySender = (
  '<md5sum of mail addr>' => 10000,
);

sub importMailExtraUtilBySender($) {
  # print $_[0] . " -> " . $importMailExtraUtilBySender{md5_hex($_[0])} . "\n";
  return ($importMailExtraUtilBySender{md5_hex(shift)} or 0);
}

# overwrite the expected utility of mails in a certain mailbox
my %inboxExtraUtil = (
  "$ENV{HOME}/.icedove/<Path to mailbox>" => 10,
);

sub importMailBaseUtilByInbox($) {
  my ($inbox) = @_;

  if(exists $inboxExtraUtil{$inbox}) {
    return $inboxExtraUtil{$inbox};
  }

  return 5;
}

my %redmineAPIKeys = (
  'https://ticket.example.com/projects/<yourproject>' => '<API Key>',
);

sub redmineAPIKey($) {
  return $redmineAPIKeys{shift()};
}

# this functian can be used to "enter" a certain location based upon time or other
# external parameters. It can also be abused to do stochastic scheduling between
# different social roles.
# create a topology as follows (use ./import-topology)
# role: * -> role_employee (100d)
# role: * -> role_pirate (100d)
# role: * -> role_intellectual (100d)
# role: role_employee -> role_employee (0s)
# role: role_pirate -> role_pirate (0s)
# role: role_intellectual -> role_intellectual (0s)
# and pretend that you are "in" the role dependent for example on time of day
# or some other fancy algorithm
sub activeSpaceBySchedule() {
  return {
    'role' => [
      space_by_name()->{'role_employee'},
      space_by_name()->{'role_pirate'},
    ]->[int(time / 3600) % 2],
  };
}

sub websites() {
  return {
    # format: "http://example.com" => [<util>, <time>, [<weekdays>]],
    # 1 = monday, 7 = sunday

    "https://github.com/Drahflow/Utilator" => [50, "3m", [1]],
  };
}

1;
