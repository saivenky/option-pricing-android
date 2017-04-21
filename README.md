# option-pricing-android

Personal project to assist with options trading. Uses Black-Scholes for pricing calculations. Main features are:

1. Price options real-time
2. Display real-time Greeks for the executed trades
3. Create Android notification when deltas should be hedged


## Input Format

Stock trades must be entered in the following format:

```
S 300 @ 56.32
```

- "S" means Stock trade
- Volume is 300
- Price is $56.32

Option trades must be entered in the following format:

```
O -3 x 57.50 C @ 0.22 2016-12-30
```

- "O" means Option trade
- Volume is -3 (i.e. short sale)
- Exercise price is $57.50
- Option type is "C" for Call (and can be "P" for Put)
- Price is $0.22
- Expiration is in YYYY-MM-DD format

## Inadequecies

- Fee structure is specific to my trading accounts
- Only handles a single stock symbol (i.e. hard-coded to "SBUX", but can be changed easily in `saivenky.optionpricer.MainActivity`)
