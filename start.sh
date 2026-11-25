#!/bin/sh

#    Openbravo POS is a point of sales application designed for touch screens.
#    Copyright (C) 2007-2009 Openbravo, S.L.
#    http://sourceforge.net/projects/openbravopos
#
#    This file is part of Openbravo POS.
#
#    Openbravo POS is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    Openbravo POS is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with Openbravo POS.  If not, see <http://www.gnu.org/licenses/>.

DIRNAME=`dirname $0`

CP=$DIRNAME/retail-pos.jar
CP=$CP:$DIRNAME/lib/*
CP=$CP:$DIRNAME/build/runtime-libs/*
CP=$CP:$DIRNAME/runtime-libs/*
CP=$CP:$DIRNAME/lib.jar
CP=$CP:$DIRNAME/locales.jar
CP=$CP:$DIRNAME/locales

# start Retail POS. Portable packages use the bundled runtime; source
# checkouts continue to use java from PATH.
JAVA=$DIRNAME/runtime/bin/java
if [ -d "$DIRNAME/runtime" ]; then
  if [ ! -x "$JAVA" ]; then
    echo "Bundled runtime Java is not executable: $JAVA" >&2
    exit 1
  fi
  JSPAWNHELPER="$DIRNAME/runtime/lib/jspawnhelper"
  if [ ! -f "$JSPAWNHELPER" ]; then
    echo "Bundled runtime jspawnhelper is missing: $JSPAWNHELPER" >&2
    exit 1
  fi
  if ! chmod 755 "$JSPAWNHELPER" || [ ! -x "$JSPAWNHELPER" ]; then
    echo "Bundled runtime jspawnhelper is not executable: $JSPAWNHELPER" >&2
    exit 1
  fi
else
  JAVA=java
fi
"$JAVA" -cp "$CP" -Djava.util.logging.config.file="$DIRNAME/logging.properties" -Ddirname.path="$DIRNAME/" com.openbravo.pos.forms.StartPOS
