#
#  Copyrights     : CNRS
#  Author         : Oleg Lodygensky
#  Acknowledgment : XtremWeb-HEP is based on XtremWeb 1.8.0 by inria : http://www.xtremweb.net/
#  Web            : http://www.xtremweb-hep.org
#   
#  This file is part of XtremWeb-HEP.
# 
#  XtremWeb-HEP is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
# 
#  XtremWeb-HEP is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
# 
#  You should have received a copy of the GNU General Public License
#  along with XtremWeb-HEP.  If not, see <http://www.gnu.org/licenses/>.
#
#

# Versionning
#  0.3 : July 7th, 2017
#    a) xwversion : 10.6.0
#  0.2 : July 3rd, 2017
#    a) exposes server ports
#

FROM ubuntu:16.04
MAINTAINER Oleg Lodygensky (oleg.lodygensky@lal.in2p3.fr)
LABEL "copyrights"="CNRS 2016"
LABEL "author"="Oleg Lodygensky"
LABEL version="0.3"
LABEL description="This creates a Docker image for the XWHEP server"
EXPOSE 4320 4321 4322 4323 4324 4325 4326 4327 4328 4329
ENV XWVERSION="10.5.2"

COPY xwhep-server-$XWVERSION.deb /tmp/
COPY xwhep-server-conf-$XWVERSION.deb /tmp/
#
# DEBIAN_FRONTEND=noninteractive bypasses user input.
# Hence, mysql-server installation will not ask
# for the new mysql admin password;
# and mysql admin password will be empty
#
RUN apt-get update && export DEBIAN_FRONTEND=noninteractive && apt-get install -y apt-utils
RUN export DEBIAN_FRONTEND=noninteractive && apt-get install -y openjdk-8-jre mysql-server mysql-client zip unzip wget make ant gcc uuid uuid-runtime vim git
RUN service mysql start

RUN cd /tmp/ && dpkg -i xwhep-server-$XWVERSION.deb
RUN cd /tmp/ && dpkg -i xwhep-server-conf-$XWVERSION.deb
RUN service mysql start && mysql -u root < /opt/xwhep-server-$XWVERSION/bin/db-maintenance/xwhep-core-tables-create-tables.sql

#
# -1- don't renice in container
# -2- we must remove LAUNCHERURL since Apache is not installed
# 
RUN sed -i "s/^V_NICE=.*//g" /opt/xwhep-server-$XWVERSION/bin/xtremwebconf.sh
RUN sed -i "s/LAUNCHER.*//g" /opt/xwhep-server-$XWVERSION/conf/xtremweb.server.conf

RUN service mysql restart && service xtremweb.server start

#RUN service mysql start && sed -i "s/^V_NICE=.*//g" /opt/xwhep-server-$XWVERSION/bin/xtremwebconf.sh && sed -i "s/LAUNCHER.*//g" /opt/xwhep-server-$XWVERSION/conf/xtremweb.server.conf && service xtremweb.server start



