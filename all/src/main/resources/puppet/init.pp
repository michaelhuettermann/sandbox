file {'/home/michaelhuettermann/work/transfer/puppet.txt':
    ensure  => present,
    content => "Hello World!",
    mode   => 700,
}
s
file { "/home/michaelhuettermann/work/transfer":
    ensure => "directory",
    owner  => "michaelhuettermann",
    group  => "michaelhuettermann",
    mode   => 750,
    before  => File['/home/michaelhuettermann/work/transfer/puppet.txt'], 
}
