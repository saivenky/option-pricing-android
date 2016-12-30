package saivenky.optionpricer;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private EditText mExpiry;
    private EditText mUnderlying;
    private EditText mStrike;
    private EditText mBid;
    private EditText mAsk;
    private EditText mVolSmile;
    private EditText mImpliedVol;
    private Button mVolCalculate;
    private Button mCalculate;
    private TextView mDisplay;
    private EditText mStrikeStep;

    private Button mDecrUnderlying;
    private Button mIncrUnderlying;
    private Button mDecrVolSmile;
    private Button mIncrVolSmile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mExpiry = (EditText) findViewById(R.id.days_to_expiry);
        mUnderlying = (EditText) findViewById(R.id.underlying);
        mStrike = (EditText) findViewById(R.id.strike);
        mBid = (EditText) findViewById(R.id.bid);
        mAsk = (EditText) findViewById(R.id.ask);

        mImpliedVol = (EditText) findViewById(R.id.implied_vol);
        mVolSmile = (EditText) findViewById(R.id.vol_smile);
        mVolCalculate = (Button) findViewById(R.id.vol_calculate);
        mCalculate = (Button) findViewById(R.id.calculate);
        mDisplay = (TextView) findViewById(R.id.display);
        mStrikeStep = (EditText) findViewById(R.id.strike_step);

        mDecrUnderlying = (Button) findViewById(R.id.decrement_underlying);
        mIncrUnderlying = (Button) findViewById(R.id.increment_underlying);
        mDecrVolSmile = (Button) findViewById(R.id.decrement_vol_smile);
        mIncrVolSmile = (Button) findViewById(R.id.increment_vol_smile);

        mVolCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calculateVol();
                hideSoftKeyboard(MainActivity.this);
            }
        });
        mCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calculate();
                hideSoftKeyboard(MainActivity.this);
            }
        });

        mDecrUnderlying.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                double underlying = Double.parseDouble(mUnderlying.getText().toString());
                mUnderlying.setText(String.format("%.2f", underlying - 0.01));
                calculate();
            }
        });

        mIncrUnderlying.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                double underlying = Double.parseDouble(mUnderlying.getText().toString());
                mUnderlying.setText(String.format("%.2f", underlying + 0.01));
                calculate();
            }
        });

        mDecrVolSmile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                double volSmile = Double.parseDouble(mVolSmile.getText().toString());
                mVolSmile.setText(String.format("%.3f", volSmile - 0.001));
                calculate();
            }
        });

        mIncrVolSmile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                double volSmile = Double.parseDouble(mVolSmile.getText().toString());
                mVolSmile.setText(String.format("%.3f", volSmile + 0.001));
                calculate();
            }
        });

        mUnderlying.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    double underlying = Double.parseDouble(mUnderlying.getText().toString());
                    double strikeStep = Double.parseDouble(mStrikeStep.getText().toString());
                    int decimals = getDecimals(strikeStep);
                    String decimalFormat = String.format("%%.%df", decimals);
                    mStrike.setText(String.format(decimalFormat, Math.round(underlying / strikeStep) * strikeStep));
                } catch(Exception e) {}
            }
        });
    }

    private int getDecimals(double number) {
        double decimal = number - (int)number;
        double precision = Math.floor(Math.log10(decimal));
        if(!(precision < 0)) return 0;
        return (int) -precision;
    }

    private void calculateVol() {
        double interest = 0;
        try {
            int daysUntilExpiry = Integer.parseInt(mExpiry.getText().toString());
            double volSmileFactor = Double.parseDouble(mVolSmile.getText().toString());
            double underlying = Double.parseDouble(mUnderlying.getText().toString());
            double strike = Double.parseDouble(mStrike.getText().toString());
            double bid = Double.parseDouble(mBid.getText().toString());
            double ask = Double.parseDouble(mAsk.getText().toString());

            BinomialModelPricer bmp = new BinomialModelPricer(daysUntilExpiry, 2);

            double vol = bmp.calculateImpliedVol((bid + ask) / 2, underlying, strike, interest, volSmileFactor);
            String displayText = String.format("%.6f", vol);

            mImpliedVol.setText(displayText);
        } catch(Exception e) {}
    }

    private void calculate() {
        try {
            int daysUntilExpiry = Integer.parseInt(mExpiry.getText().toString());
            double volSmileFactor = Double.parseDouble(mVolSmile.getText().toString());
            double underlying = Double.parseDouble(mUnderlying.getText().toString());
            double strike = Double.parseDouble(mStrike.getText().toString());
            double vol = Double.parseDouble(mImpliedVol.getText().toString());

            double strikeIncrement = Double.parseDouble(mStrikeStep.getText().toString());
            double lowStrike = strike - 4 * strikeIncrement;
            double highStrike = strike + 4 * strikeIncrement;

            BinomialModelPricer bmp = new BinomialModelPricer(daysUntilExpiry, 2);

            String displayText = "";
            for (double p = lowStrike; p <= highStrike; p += strikeIncrement) {
                double callPrice = bmp.calculateCallPrice(underlying, p, vol, volSmileFactor);
                double putPrice = bmp.calculatePutPrice(underlying, p, vol, volSmileFactor);
                displayText += String.format("%.2f: %f\t%f\n", p, callPrice, putPrice);
            }

            mDisplay.setText(displayText);
        } catch(Exception e) {}
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                activity.getCurrentFocus().getWindowToken(), 0);
    }
}