file {'/Users/michaelh/work/data/share/transfer/puppet.txt':
    ensure  => present,
    content => "Hello World!",
    mode   => "700",
}

file { "/Users/michaelh/work/data/share/transfer":
    ensure => "directory",
    owner  => "michaelh",
    group  => "staff",
    mode   => "770",
    before  => File['/Users/michaelh/work/data/share/transfer/puppet.txt'], 
}
