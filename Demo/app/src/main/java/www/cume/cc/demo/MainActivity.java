package www.cume.cc.demo;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import com.peersafe.btrsdk.api.AccountTransactionCallback;
import com.peersafe.btrsdk.api.BTRSdkApi;
import com.peersafe.btrsdk.api.BalanceInfoCallback;
import com.peersafe.btrsdk.api.ConnectDelegate;
import com.peersafe.btrsdk.api.CurrencyTxDetail;
import com.peersafe.btrsdk.api.CurrencyTxDetails;
import com.peersafe.btrsdk.api.CurrencyTxsInfoCallback;
import com.peersafe.btrsdk.api.SubscribeResultCallback;
import com.peersafe.btrsdk.api.TransferFeeCallback;
import com.peersafe.btrsdk.api.TransferInfo;
import com.peersafe.btrsdk.api.WalletInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements ConnectDelegate {

    private final static String TAG = MainActivity.class.getSimpleName();

    //节点的地址，比特米发行地址，连接状态回调，账户交易通知回调。
    public final static String CHAIN_SQL_NODE_ADDR = "ws://btnn.cume.cc:6006";
    //比特米发行地址
    public final static String ISSUE_ADDR = "znEwBEkpenKWhL9wFXeyBvpiVu4uK4UuCr";
    private BTRSdkApi mBtrSdkApi = new BTRSdkApi();
    private boolean isSdkConnected = false;
    //当前操作的钱包
    private WalletInfo mCurWalletInfo;
    //交易明细集合
    private ArrayList<CurrencyTxDetail> mCurrencyTxDetails = new ArrayList<>();
    private ArrayAdapter<CurrencyTxDetail> mDetailAdapter;
    private String mMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        init();
    }

    private void init() {
        /*默认当前钱包*/
        mCurWalletInfo = new WalletInfo("", "", "");
        //初始化sdk，与账户无关
        mBtrSdkApi.sdkInit(CHAIN_SQL_NODE_ADDR, ISSUE_ADDR, MainActivity.this);
        mDetailAdapter =new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mCurrencyTxDetails);
    }


    @OnClick({R.id.btn_import, R.id.btn_trusted_test, R.id.btn_get_sys_balance,R.id.btn_get_btr_balance,
            R.id.btn_transfer, R.id.btn_tx_detail, R.id.btn_transfer_fee,R.id.btn_connect_state})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_import:
                importAccount();
                break;
            case R.id.btn_trusted_test:
                subscribeAccountTransaction();
                break;
            case R.id.btn_get_sys_balance:
                getSysCoinBalance();
                break;
            case R.id.btn_get_btr_balance:
                getIssueCurrencyBalance();
                break;
            case R.id.btn_transfer:
                transferCurrency();
                break;
            case R.id.btn_tx_detail:
                mCurrencyTxDetails.clear();
                getIssueCurrencyTxDetail(mCurWalletInfo.getWalletAddr(),5,"");
                break;
            case R.id.btn_transfer_fee:
                getTransferFee();
                break;
            case R.id.btn_connect_state:
                showMessageDialog("主动获取当前sdk连接状态","已经连接上？"+isConnected());
                break;
        }
    }

    /**
     * 从本地导入比特米账号
     * 主意：该余额和名称不是实时的，并且私钥是与密码进行了异或加密的
     */
    private void importAccount() {
        JSONObject jsonObject = Utils.readJsonAssets(this);
        try {
            mCurWalletInfo.setWalletAddr(jsonObject.getString("walletAddr"));
            mCurWalletInfo.setPublicKey(jsonObject.getString("publicKey"));
            mCurWalletInfo.setPrivateKey(jsonObject.getString("privateKey"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        showMessageDialog("导入账号信息",jsonObject.toString());
        Log.i(TAG, "导入账号信息:" + jsonObject.toString());
    }

    /**
     * 测试用已经信任的账户初始化，并且已经有比特米余额的货币
     * （该流程用于切换账户，只有走了该流程，才可以实时的收到其他人给这个账号转账的回调，如果只是查询，不需要调用注册）
     */
    private void subscribeAccountTransaction() {
        if (isSdkConnected) {
            mBtrSdkApi.subscribeAccountTransaction(mCurWalletInfo.getWalletAddr(), new SubscribeResultCallback() {
                @Override
                public void subscribeResult(int i, String s, int i1) {
                    Log.i(TAG, "subscribeResult code is:" + i);
                    Log.i(TAG, "subscribeResult message is:" + s);
                    Log.i(TAG, "subscribeResult result is:" + i1);

                    final int result = i1;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showMessageDialog("注册交易通知结果", "结果:" + (result==0?"切换成功":"切换失败"));
                        }
                    });
                }
            }, new AccountTransactionCallback() {
                @Override
                public void accountTransactionResult(int i, String s, TransferInfo transferInfo) {
                    Log.i(TAG, "accountTransactionResult code is:" + i);
                    Log.i(TAG, "accountTransactionResult message is:" + s);
                    Log.i(TAG, "accountTransactionResult transferInfo is:" + transferInfo.toString());

                    final TransferInfo transInfo = transferInfo;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showMessageDialog("收到比特米转入", "转入信息:" + transInfo.toString());
                        }
                    });
                }
            });
        }
    }

    /**
     * 查询交易明细
     * 注意：第一次查询marker为空，如果marker不为空则代表还有数据，
     *        下次查询时把marker作为参数传进去；
     * @param walletAddr 账户地址
     * @param number 查询数量
     * @param marker 根据marker查询下一批的交易明细
     */
    public void getIssueCurrencyTxDetail(String walletAddr, int number, String marker){
        mBtrSdkApi.getIssueCurrencyTxDetail(walletAddr, number, marker, new CurrencyTxsInfoCallback() {
            @Override
            public void currencyTxsInfo(int i, String s, CurrencyTxDetails currencyTxDetails) {
                final StringBuilder stringBuilder = new StringBuilder()
                        .append("code:").append(i)
                        .append("\nmessage:").append(s);
                if (null != currencyTxDetails && currencyTxDetails.getMarker() != null) {
                    stringBuilder.append("\nmarker:").append(currencyTxDetails.getMarker());
                    mMarker = currencyTxDetails.getMarker();
                }

                if (null != currencyTxDetails) {
                    for (int j = 0; j < currencyTxDetails.getCurrencyTxDetailList().size(); j++) {
                        stringBuilder.append("\n").append(currencyTxDetails.getCurrencyTxDetailList().get(j).toString());
                        mCurrencyTxDetails.add(currencyTxDetails.getCurrencyTxDetailList().get(j));
                    }
                }
                Log.i(TAG,"每次查询的交易明细: "+stringBuilder.toString());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showDetailsDialog();
                    }
                });
            }
        });
    }

    /**
     * 显示交易记录明细
     */
    private void showDetailsDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("显示交易明细");
        builder.setAdapter(mDetailAdapter,null);
        builder.setPositiveButton("查询更多", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface anInterface, int i) {
                if(!TextUtils.isEmpty(mMarker)){
                    getIssueCurrencyTxDetail(mCurWalletInfo.getWalletAddr(),2,mMarker);
                }else {
                    Toast.makeText(getApplicationContext(),"没有交易数据了。。。",Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("取消",null);
        builder.show();
    }

    /**
     * 默认账户对目标账户进行转账，确保调用 subscribeAccountTransaction()成功
     * 测试toAddress：zN7nbEH3EDPLQBJ6rgTUWVVF6pNZptjPpc
     */
    private void transferCurrency() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("输入转账信息");
        View view = View.inflate(this, R.layout.dialog_transfer, null);
        final EditText toAddr = view.findViewById(R.id.et_addr);
        final EditText amount = view.findViewById(R.id.et_amount);
        final EditText pwd = view.findViewById(R.id.et_pwd);
        builder.setView(view);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface anInterface, int i) {
                //检验用户输入密码是否正确
                String temPwd = pwd.getText().toString();
                String tempPrivatekey = Utils.deCodePrivate(mCurWalletInfo.getPrivateKey(), temPwd);
                if(TextUtils.isEmpty(tempPrivatekey)){
                    showMessageDialog("比特米转账信息","解密私钥出错");
                   return;
                }
                if (!tempPrivatekey.startsWith(Constant.strWalletPre)) {
                    showMessageDialog("比特米转账信息","输入密码错误");
                    return;
                }
                String realPrivatekey = tempPrivatekey.substring(Constant.strWalletPre.length(), tempPrivatekey.length());
                String tempToAddr = toAddr.getText().toString();
                String tempAmount = amount.getText().toString();
                Log.e(TAG,"======realPrivatekey: "+tempPrivatekey);
                Log.e(TAG,"======tempToAddr: "+tempToAddr);
                Log.e(TAG,"======tempAmount: "+tempAmount);
                if(!TextUtils.isEmpty(tempToAddr) && !TextUtils.isEmpty(tempAmount)){
                    mBtrSdkApi.transferCurrency(realPrivatekey,tempToAddr, tempAmount,
                            new AccountTransactionCallback() {
                                @Override
                                public void accountTransactionResult(int i, String s, TransferInfo transferInfo) {
                                    final StringBuilder message = new StringBuilder()
                                            .append(i==0?"转账成功":"转账失败").append("\n")
                                            .append("code:").append(i)
                                            .append("\nmessag:").append(s)
                                            .append("\ntransferInfo:").append(transferInfo.toString());
                                    Log.i(TAG, "比特米转账信息: "+message.toString());
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            showMessageDialog("比特米转账信息",message.toString());
                                        }
                                    });
                                }
                            });
                }else {
                    Log.i(TAG, "比特米余额信息: 输入有误");
                    showMessageDialog("比特米余额信息","输入有误");
                }
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }


    /**
     * 获取转账手续费
     */
    public void getTransferFee(){
        mBtrSdkApi.getTransferFee(new TransferFeeCallback() {
            @Override
            public void transferFeeInfo(int i, String s, String s1) {
                final StringBuilder message = new StringBuilder()
                        .append("code:").append(i)
                        .append("\nmessag:").append(s)
                        .append("\nTransferFee:").append(s1);
                Log.i(TAG, "转账手续费信息: "+message.toString());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showMessageDialog("转账手续费信息",message.toString());
                    }
                });
            }
        });
    }

    /**
     * 获取比特米余额（获取比特米不用进行信任切换）
     */
    public void getIssueCurrencyBalance(){
        mBtrSdkApi.getIssueCurrencyBalance(mCurWalletInfo.getWalletAddr(), new BalanceInfoCallback() {
            @Override
            public void balanceInfo(int i, String s, String s1) {
                //code：0表示查询成功,3代表账户已被冻结
                final StringBuilder message = new StringBuilder()
                        .append("code:").append(i)
                        .append("\nmessag:").append(s)
                        .append("\nbalance:").append(s1);
                Log.i(TAG, "比特米余额信息: "+message.toString());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showMessageDialog("比特米余额信息",message.toString());
                    }
                });
            }
        });
    }

    /**
     * 获取系统币余额
     */
    private void getSysCoinBalance() {
        mBtrSdkApi.getSysCoinBalance(mCurWalletInfo.getWalletAddr(), new BalanceInfoCallback() {
            @Override
            public void balanceInfo(int i, String s, String s1) {
                final StringBuilder message = new StringBuilder()
                        .append("code is:").append(i)
                        .append("\nmessage is:").append(s)
                        .append("\nbalance is:").append(s1);
                Log.i(TAG, "系统币余额信息：" + message.toString());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showMessageDialog("系统币余额信息",message.toString());
                    }
                });
            }
        });
    }

    /**
     * 显示对话框
     * @param title 显示标题
     * @param message 显示内容
     */
    private void showMessageDialog(String title, String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.show();
    }

    /**
     * 回调当前sdk连接状态
     * @param i 0为连接成功
     */
    @Override
    public void connectState(int i) {
        isSdkConnected = i==CONNECT_SUCCESS;
        Log.i(TAG, "!!!connectState:" + i+", isConnected: "+isSdkConnected);
    }

    /**
     * 主动获取判断当前sdk是否已经开启
     * @return true为已连接，否则未连接
     */
    public boolean isConnected(){
        isSdkConnected = mBtrSdkApi.getConnectState()==CONNECT_SUCCESS;
        Log.i(TAG, "isSdkConnected: "+isSdkConnected);
        return isSdkConnected;
    }
}
