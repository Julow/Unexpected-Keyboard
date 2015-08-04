#!/usr/bin/python
# **************************************************************************** #
#                                                                              #
#                                                         :::      ::::::::    #
#    logcat.py                                          :+:      :+:    :+:    #
#                                                     +:+ +:+         +:+      #
#    By: juloo <juloo@student.42.fr>                +#+  +:+       +#+         #
#                                                 +#+#+#+#+#+   +#+            #
#    Created: 2015/08/04 18:31:55 by juloo             #+#    #+#              #
#    Updated: 2015/08/04 20:10:14 by juloo            ###   ########.fr        #
#                                                                              #
# **************************************************************************** #

from subprocess import Popen, PIPE
from sys import argv, stdout, exit
import re

if len(argv) <= 1:
	print("\033[31mNot enougth argument\033[39m")
	print("Usage: %s <app name (package name)>" % argv[0])
	exit()

prefixReg = re.compile('[^:]+\(\s*([0-9]+)\):.*')
startAppReg = re.compile('[^:]+: Start proc ([^ ]+)[^:]+:\s*pid\s*=\s*([0-9]+)\s*.*')

app_name = argv[1]
pids = []

Popen("adb logcat -c".split()).wait()

cmd = Popen("adb shell ps".split(), stdout=PIPE)
for l in cmd.stdout:
	if app_name in l:
		pids.append(int(l.split()[1]))

if len(pids) <= 0:
	stdout.write("\033[91mApp not started. Waiting")
else:
	stdout.write("\033[32mApplication pid:")
	for p in pids:
		stdout.write(" %d" % p)
stdout.write("\033[39m\n")
stdout.flush()

cmd = Popen("adb logcat".split(), stdout=PIPE)
for l in iter(cmd.stdout.readline, ""):
	m = startAppReg.match(l)
	if m != None and app_name in m.group(1):
		pid = int(m.group(2))
		if pid not in pids:
			pids.append(pid)
			stdout.write("\033[32mApplication started. pid:")
			for p in pids:
				stdout.write(" %d" % p)
			stdout.write("\033[39m\n")
			stdout.flush()
	m = prefixReg.match(l)
	if m != None and int(m.group(1)) in pids:
		stdout.write(l)
		stdout.flush()
