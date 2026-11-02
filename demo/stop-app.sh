#!/usr/bin/env bash
pkill -f 'gov.demo.vouchers.VoucherApplication' 2>/dev/null
pkill -f 'spring-boot:run' 2>/dev/null
echo stopped
