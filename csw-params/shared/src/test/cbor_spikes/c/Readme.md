##Installation

### Homebrew

```bash
brew tap pjk/libcbor
brew install libcbor
```

### Ubuntu 18.04 and above

```bash
sudo add-apt-repository universe
sudo apt-get install libcbor-dev
```

### Fedora & RPM friends

```bash
yum install libcbor-devel
```

For usage example, see the `sample.c` file.

##Compilation

```console
$   gcc sample.c -lcbor -o serialized-cbor
$   ./serialized-cbor
```