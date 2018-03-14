# btrdemo

- 开发环境： 使用Android Studio，支持Android 4.0及以上版本。
- 配置环境：1、SDK包中libs下提供的btrsdk.aar包拷贝到app/libs目录下，然后在app的build.gradle下按下图所示方式添加：
![image](https://note.youdao.com/yws/api/personal/file/B400E5AC623A43599D01C8D26667BB0B?method=download&shareKey=c5dc3096897d5a1fb0bd1a45a2fcb867)

    2、在AndroidManifest.xml文件下添加权限。
<uses-permission android:name="android.permission.INTERNET"/>

- 集成SDK，BTRSdkApi关键类的API


```
/**
 *  SDK初始化
 *  (该sdk的一切操作基于sdk已经初始化成功)
 *
 * @param chainSqlNodeAddr chainsql节点的地址
 * @param issueAddr  比特米发行地址
 * @param connectDelegate  sdk连接状态回调
 */
public void sdkInit(String chainSqlNodeAddr, String issueAddr, ConnectDelegate connectDelegate)
```



```
/**
 * SDK关闭
 * (当需要清理SDK连接相关信息时，可以调用SDK关闭接口)
 */
public void sdkClose()
```



```

/**
 * 注册账户交易通知
 * (当需要实时的监听账户的交易通知，即他人给我转账的回调时，需要在sdk成功初始化的前提下调用注册账户交易通知的接口)
 *
 * @param walletAddr 钱包地址
 * @param subscribeResultCallback  账户的注册交易通知结果回调
 * @param accountTransactionCallback  账户交易通知回调(用于app上层异步接收他人转账比特米的信息）)
 */
public void subscribeAccountTransaction(String walletAddr, SubscribeResultCallback subscribeResultCallback, AccountTransactionCallback accountTransactionCallback)
```



```
/**
 * 查询系统币余额接口
 * (当初始化成功，查询得到系统币为0说明该账号不可用)
 *
 * @param walletAddr 钱包地址
 * @param balanceInfoCallback 查询通知回调(调用的结果以异步方式返回给app上层，详细请看demo)
 */
public void getSysCoinBalance(String walletAddr, BalanceInfoCallback balanceInfoCallback)
```



```
/**
 * 查询比特米余额接口
 * （可以先判断是否有系统币判断该账户是否可用）
 *
 * @param walletAddr 钱包地址
 * @param balanceInfoCallback 查询通知回调(调用的结果以异步方式返回给app上层，详细请看demo)
 */
public void getIssueCurrencyBalance(String walletAddr, BalanceInfoCallback balanceInfoCallback)
```



```
/**
 * 转账
 * (如果本地账户不止一个，要确保调用 subscribeAccountTransaction()切换成功)
 *
 * @param privateKey 转账方的私钥
 * @param toWalletAddr 接收方的地址
 * @param amount 转账数额
 * @param accountTransactionCallback 转账通知回调(调用的结果以异步方式返回给app上层，详细请看demo)
 */
public void transferCurrency(String privateKey, String toWalletAddr, String amount, AccountTransactionCallback accountTransactionCallback)
```




```
  /**
 * 查询比特米交易明细
 * (当初次请求时marker传入空，limit为获取的数目，当总的交易数多于limit时，会返回marker字段，当要做加载更多功能时，
 * 可以传入marker，则服务端会返回该marker之前的交易记录。注：因为sdk底层过滤了系统币转入的明细，
 * 因此会导致实际返回的数目可能小于传入的limit。)
 *
 * @param walletAddr 钱包地址
 * @param limit 查询数量
 * @param marker 根据marker是否为空判断是否还有剩余的交易记录
 * @param currencyTxsInfoCallback 查询通知回调(调用的结果以异步方式返回给app上层，详细请看demo)
 */
public void getIssueCurrencyTxDetail(String walletAddr, int limit, String marker, CurrencyTxsInfoCallback currencyTxsInfoCallback)
```




```
/**
 * 获取转账手续费
 * （手续费是sdk默认从转出方扣除）
 *
 * @param transferFeeCallback 查询通知回调(调用的结果以异步方式返回给app上层，详细请看demo)
 */
public void getTransferFee(TransferFeeCallback transferFeeCallback)
```



```
/**
 * 获取sdk连接状态
 * (进行交易前可以主动获取当前sdk连接状态，判断是否已经成功初始化)
 *
 * @return 0代表连接成功，否则连接失败
 */
public int getConnectState()
```

