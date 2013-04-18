package Configuration;

# default informations

my $ownName = '<Your Name>';
my $ownEmailAddress = '<Your Email Address>';

sub getName() {
  return $ownName;
}


sub getEmail() {
  return $ownEmailAddress;
}

1;
