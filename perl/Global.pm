package Global;

use Exporter;
@ISA = qw(Exporter);
@EXPORT_OK = qw(dbh create_gid iso_date iso_full_date interpretUnit interpretUnitExact reverseInterpretUnit PUBLICATION_LOCAL PUBLICATION_PRIVATE PUBLICATION_MASKED PUBLICATION_TRANSPARENCY PUBLICATION_OFFER space_by_name);

use strict;
use warnings;

use DBI;
use Date::Format;

my $dbh;

sub dbLocation() {
  if(defined $ENV{'HOME'} and $ENV{'HOME'} ne '/root') {
    return "$ENV{'HOME'}/utilator.db";
  }
  if(-d '/opt/utilator') {
    return '/opt/utilator/utilator.db';
  }
}

sub dbh() {
  return $dbh if $dbh;

  return $dbh = DBI->connect("dbi:SQLite:dbname=" . dbLocation() . "", "", "", {
      RaiseError     => 1,
      sqlite_unicode => 1,
    });
}

sub create_gid() {
  my $template = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx';
  1 while($template =~ s/x/[split m!!, "0123456789ABCDEF"]->[int(rand 16)]/e);
  1 while($template =~ s/y/[split m!!, "89AB"]->[int(rand 4)]/e);
  return $template;
}

sub iso_date($) {
  return time2str('%Y-%m-%d', shift);
}

sub iso_full_date($) {
  return time2str('%Y-%m-%dT%H:%M:%S', shift);
}

sub interpretUnit {
  my ($str) = @_;
  
  $str =~ /(?'amount'[0-9.]+)\s*(?'unit'[wdhms])/ or die "bad time spec: $str";

  return $+{'amount'} * {
      's' => 1,
      'm' => 60,
      'h' => 60 * 60,
      'd' => 60 * 60 * 12,
      'w' => 60 * 60 * 10 * 7,
    }->{$+{'unit'}};
}

sub interpretUnitExact {
  my ($str) = @_;
  
  $str =~ /(?'amount'[0-9.]+)\s*(?'unit'[wdhms])/ or die "bad time spec: $str";

  return $+{'amount'} * {
      's' => 1,
      'm' => 60,
      'h' => 60 * 60,
      'd' => 60 * 60 * 24,
      'w' => 60 * 60 * 24 * 7,
    }->{$+{'unit'}};
}

sub reverseInterpretUnit($) {
  my ($time) = @_;

  if($time < 60 * 2) {
    return $time . "s";
  } elsif($time < 60 * 60 * 2) {
    return $time / 60 . "m";
  } elsif($time < 60 * 60 * 24 * 2) {
    return $time / 60 / 60 . "h";
  } elsif($time < 60 * 60 * 12 * 9) {
    return $time / 60 / 60 / 12 . "d";
  } elsif($time < 60 * 60 * 10 * 5 * 100) {
    return $time / 60 / 60 / 10 / 5 . "w";
  } else {
    return $time / 60 / 60 / 10 / 5 / 52 . "y";
  }     
} 

my $space_by_name;

sub space_by_name() {
  return $space_by_name if($space_by_name);

  $space_by_name = {};

  foreach my $row (@{dbh()->selectall_arrayref(<<EOSQL, { Slice => {} })}) {
  SELECT * FROM space_active
EOSQL
    $space_by_name->{$row->{'name'}} = $row;
  }

  return $space_by_name;
}

use constant PUBLICATION_LOCAL => 0;
use constant PUBLICATION_PRIVATE => 1;
use constant PUBLICATION_MASKED => 2;
use constant PUBLICATION_TRANSPARENCY => 3;
use constant PUBLICATION_OFFER => 4;

1;
