package com.example.suvp.shop.Activity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.suvp.shop.Fragments.DatePickerFragment;
import com.example.suvp.shop.Fragments.ItemListFragment;
import com.example.suvp.shop.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import DataBase.ManagedObjects.Invoice;
import DataBase.ManagedObjects.Product;
import General.CustomProductItemListAdapter;
import General.CustomProductListAdapter;

/**
 * Created by suvp on 3/16/2016.
 */
public class AddInvoiceActivity extends FragmentActivity
{
    private final String LOG_TAG = getClass().getSimpleName();
    private final Context context_ = this;
    public final static String SERIALIZED_PRODUCT = "productPassed";

    Date selectedDate = new Date();
    CustomProductListAdapter customListAdapter;

    static final int REQUEST_CODE_FOR_SELECTE_PRODUCT = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_invoice);

        setDateButtonActionListener();
        setAddButtonActionListener();
        setListActionListener();
        setSaveActionListener();

        Log.i(LOG_TAG, "Invoice Menu Create");
    }

    private void setDateButtonActionListener()
    {
        Button lSelectButton = (Button)findViewById(R.id.buttonSelectDate);
        lSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });
        TextView lDateSelectedTextView = (TextView)findViewById(R.id.textFieldDate);

        StringBuffer lSelectedDate = new StringBuffer();
        lSelectedDate.append(selectedDate.getDay()).append(":").append(selectedDate.getMonth()).append(":").append(selectedDate.getYear());
        lDateSelectedTextView.setText(lSelectedDate);
    }

    //whenever a datepicker dialog is created we have to pass the callback date picked object also so that the date is received back.
    private void showDatePicker() {
        DatePickerFragment date = new DatePickerFragment();
        /**
         * Set Up Current Date Into dialog
         */
        Calendar calender = Calendar.getInstance();
        Bundle args = new Bundle();
        args.putInt("year", calender.get(Calendar.YEAR));
        args.putInt("month", calender.get(Calendar.MONTH));
        args.putInt("day", calender.get(Calendar.DAY_OF_MONTH));
        date.setArguments(args);
        /**
         * Set Call back to capture selected date
         */
        date.setCallBack(ondate);
        date.show(getSupportFragmentManager(), "Date Picker");
    }

    //The listener for the datepicker fragment or the dialog
    DatePickerDialog.OnDateSetListener ondate = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth)
        {
            TextView lDateSelectedTextView = (TextView)findViewById(R.id.textFieldDate);
            StringBuffer lSelectedDate = new StringBuffer();
            lSelectedDate.append(dayOfMonth).append(":").append(monthOfYear).append(":").append(year);
            lDateSelectedTextView.setText(lSelectedDate);
            selectedDate = new Date(year, monthOfYear,dayOfMonth);
        }
    };

    private void setAddButtonActionListener()
    {
        Button lAddButton = (Button)findViewById(R.id.buttonAddItem);
        lAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent lSearchProductActivity = new Intent(context_, SearchProductActivity.class);
                startActivityForResult(lSearchProductActivity, REQUEST_CODE_FOR_SELECTE_PRODUCT);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_FOR_SELECTE_PRODUCT) {
            if (resultCode == RESULT_OK) {
                Object[] lReceivedProducts = (Object[])data.getExtras().get(SearchProductActivity.SERIALIZED_PRODUCTS_FINALY_SELECTED);
                List<Product> lSelectedProducts = new ArrayList<>();
                for(Object lProduct : lReceivedProducts)
                {
                    lSelectedProducts.add((Product) lProduct);
                }

                customListAdapter.addAll(lSelectedProducts);
                customListAdapter.notifyDataSetChanged();
                Log.i(LOG_TAG, "Selected:"+lSelectedProducts.size());
            }
            else if(resultCode == RESULT_CANCELED)
            {
                Toast.makeText(this, "No Item Selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setListActionListener()
    {
        ListView listView = (ListView)findViewById(R.id.itemList);
        customListAdapter = new CustomProductListAdapter(this, new LinkedList<Product>());
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                List<Product> lProductList = customListAdapter.getProductList();
                Product lSelectedProduct = lProductList.get(position);
                Intent singleProductViewActivity = new Intent(context_, ProductActivity.class);
                singleProductViewActivity.putExtra(SERIALIZED_PRODUCT, lSelectedProduct);
                startActivity(singleProductViewActivity);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i(LOG_TAG, "Long click of the item");

                final CharSequence[] items = {"Delete"};
                final List<Product> lProductList = customListAdapter.getProductList();

                final Product lSelectedProduct = lProductList.get(position);
                AlertDialog.Builder builder = new AlertDialog.Builder(context_);

                builder.setTitle("Action:");
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        lProductList.remove(lSelectedProduct);
                        customListAdapter.notifyDataSetChanged();
                    }
                });

                AlertDialog alert = builder.create();
                alert.show();
                //Says if the event is consumed or should be propogated .
                return true;
            }
        });
        listView.setAdapter(customListAdapter);
    }

    private void setSaveActionListener()
    {
        Button lSaveButton = (Button)findViewById(R.id.buttonAddSave);
        lSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Product> productList = customListAdapter.getProductList();
                TextView lTextView = (TextView)findViewById(R.id.textViewInvoiceNumber);
                String lInvoiceNumber = (String)lTextView.getText();
                Boolean lIsValid = validate(productList, lInvoiceNumber);
                if(lIsValid)
                {
                    Invoice lNewInvoice = new Invoice();
                    lNewInvoice.setInvoiceNumber(lInvoiceNumber);
                    lNewInvoice.setDate(selectedDate);
                }
            }
        });

    }

    private boolean validate(List<Product> aInProductList, String aInInvoiceNumber)
    {
        StringBuffer lErrorMesage = new StringBuffer();
        if(aInInvoiceNumber == null || aInInvoiceNumber.isEmpty())
        {
            lErrorMesage.append("Please Select Invoice Number.\n") ;
        }

        if(aInProductList == null || aInProductList.size() <=0)
        {
            lErrorMesage.append("Please Select Product.\n") ;
        }

        if(lErrorMesage.length() <= 0)
        {
            Toast.makeText(this, lErrorMesage, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
