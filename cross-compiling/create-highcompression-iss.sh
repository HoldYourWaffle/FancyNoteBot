#!/bin/bash
$(cd $( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd ))
cat create-setup.iss > create-setup-highcompression.iss
echo "
; Compression
[Setup]
Compression=lzma2/ultra64
LZMAAlgorithm=1
LZMAMatchFinder=BT
SolidCompression=yes
LZMANumBlockThreads=1
LZMANumFastBytes=273
LZMADictionarySize=1048576
LZMAUseSeparateProcess=yes
InternalCompressLevel=ultra64" >> create-setup-highcompression.iss
