#!/bin/sh

# Reset docsig to the default values initially used.
# This script should not be run unless there was a
# serious problem with the configuration.

cp /etc/docsig/docsig.config /usr/app/docsig.config
rm -f /usr/app/keystore.jks
rm -fr /etc/acme/*
rm -fr /var/www.well-known/acme-challenge
