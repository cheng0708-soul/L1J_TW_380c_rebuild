#!/bin/bash
ss -tlnp | grep 2000 && tail -1 server.log || echo 'DOWN'
