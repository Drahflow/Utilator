package Configuration;

use Data::Dumper;

eval {
  require Settings;
};
if($@) {
  die "Could not load Settings.pm\n\n####>>>> maybe rename Settings.example.pm <<<<####\n\n$@";
}
import Settings;

eval {
  require Secrets;
};
if($@) {
  die "Could not load Secrets.pm\n\n####>>>> maybe rename Secrets.example.pm <<<<####\n\n$@";
}
import Secrets;

1;
