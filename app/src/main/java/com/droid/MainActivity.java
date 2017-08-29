package com.droid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.droid.MyLetterListView.OnTouchingLetterChangedListener;

public class MainActivity extends Activity {
	private BaseAdapter adapter;  
    private ListView personList;
    private TextView overlay;
    private MyLetterListView letterListView;
    private AsyncQueryHandler asyncQuery;  
    private static final String NAME = "name", NUMBER = "number", SORT_KEY = "sort_key";
    private HashMap<String, Integer> alphaIndexer;//存放存在的汉语拼音首字母和与之对应的列表位置
    private String[] sections;//存放存在的汉语拼音首字母
    private Handler handler;
    private OverlayThread overlayThread;

    private final boolean isSimulatedData=true;//是否使用模拟数据
  
    @Override  
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);  
        setContentView(R.layout.main);
        LogUtil.logWithMethod(new Exception());

        personList = (ListView) findViewById(R.id.list_view);
        letterListView = (MyLetterListView) findViewById(R.id.MyLetterListView01);
        letterListView.setOnTouchingLetterChangedListener(new LetterListViewListener());
        
        asyncQuery = new MyAsyncQueryHandler(getContentResolver());
        alphaIndexer = new HashMap<String, Integer>();
        handler = new Handler();
        overlayThread = new OverlayThread();
        initOverlay();
    }
  
    @Override  
    protected void onResume() {  
        super.onResume();
        LogUtil.logWithMethod(new Exception());
        if(isSimulatedData){
            addListFromFile();
            LogUtil.logWithMethod(new Exception());
        } else {
            Uri uri = Uri.parse("content://com.android.contacts/data/phones");
            String[] projection = {"_id", "display_name", "data1", "sort_key"};
            asyncQuery.startQuery(0, null, uri, projection, null, null,
                    "sort_key COLLATE LOCALIZED asc");
        }
    }  
  
    //查询联系人
    private class MyAsyncQueryHandler extends AsyncQueryHandler {  
  
        public MyAsyncQueryHandler(ContentResolver cr) {  
            super(cr);  
  
        }  
  
        @Override  
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {  
            if (cursor != null && cursor.getCount() > 0) {  
                List<ContentValues> list = new ArrayList<ContentValues>();  
                cursor.moveToFirst();  
                for (int i = 0; i < cursor.getCount(); i++) {
                    ContentValues cv = new ContentValues();  
                    cursor.moveToPosition(i);  
                    String name = cursor.getString(1);  
                    String number = cursor.getString(2);  
                    String sortKey = cursor.getString(3);
                    if (number.startsWith("+86")) {  
                        cv.put(NAME, name);  
                        cv.put(NUMBER, number.substring(3));  //去掉+86
                        cv.put(SORT_KEY, sortKey);  
                    } else {  
                        cv.put(NAME, name);  
                        cv.put(NUMBER, number);  
                        cv.put(SORT_KEY, sortKey);  
                    }  
                    list.add(cv);  
                }  
                if (list.size() > 0) {  
                    setAdapter(list);  
                }  
            }  
        }
    }  

    private void addListFromFile(){
        List<ContentValues> list = new ArrayList<ContentValues>();

        try {
            List<String> listName = readFileFromAssets(MainActivity.this, "names.txt");
            LogUtil.logWithMethod(new Exception(),"listName="+listName.toString());

            List<String> listTemp = new ArrayList<String>();
            for(String str : listName){
                //去掉空行
                if(str.trim().length()>0) {
                    //去掉行内字符串的首尾空格
                    listTemp.add(str.trim());
                }
            }
            listName.clear();
            listName.addAll(listTemp);

            //进行联系人的排序
            Collections.sort(listName, new SortComparator());
//            Collections.sort(listName, new SortComparator0());
            LogUtil.logWithMethod(new Exception(),"listName="+listName.toString());

            int index=0;
            for(String str : listName){
                ContentValues cv = new ContentValues();
                String name = str;
                String number = String.format("138%08d",index+1);//模拟电话号码
                String sortKey = str;//mapTemp.get("name");//其实可以使用拼音，不过，为了和通讯录的情况匹配，所以还是使用汉字
                cv.put(NAME, name);
                cv.put(NUMBER, number);
                cv.put(SORT_KEY, sortKey);
                list.add(cv);
                index++;
            }

        } catch (Exception e){
            e.printStackTrace();
        }

        if (list.size() > 0) {
            setAdapter(list);
        }
    }


    /**
     * 按字符升序排序，最初的排序方法
     * */
    class SortComparator0 implements Comparator {
        @Override
        public int compare(Object t1, Object t2) {
            String a=(String)t1;
            String b=(String)t2;
            int flag=a.compareTo(b);
            if (flag==0){
                return a.compareTo(b);
            }else{
                return flag;
            }
        }
    };

    /**
     * 联系人排序，最终的排序方法
     * */
    private int compareCallNum=0;//判读是否是第一层的比较（递归调用中）
    class SortComparator implements Comparator {
        @Override
        public int compare(Object lhs, Object rhs) {

            compareCallNum = 0;
            return compareString((String)lhs,(String)rhs);

        }
    }

    //只比较一个字符，递归调用
    public int compareString(String lhs, String rhs) {

        compareCallNum++;

        //判断第一个字符，汉字最前，其次字母，然后是数字，若有其他符号，放在最后

        String nameA = lhs;//.trim();//注意，由于递归调用，所以此处不能再使用trim了
        String nameB = rhs;//.trim();

        //若存在长度为0的情况：
        if((nameA.length()==0)&&(nameB.length()==0)){
            return 0;
        } else if(nameA.length()==0){
            return -1;
        } else if(nameB.length()==0){
            return 1;
        }

        String firstStrA = nameA.substring(0,1);
        String firstStrB = nameB.substring(0,1);

        //先从类型上来区分：汉字>字母>数字>其他符号，若类型不同，立即出比较的结果
        //但是汉字与字母，由于存在首字母的分段，所以先不区分开
        int typeA = getFirstCharType(nameA);
        int typeB = getFirstCharType(nameB);
        if(typeA>typeB){
            LogUtil.logWithMethod(new Exception(),"nameA="+nameA+" nameB="+nameB);
            return -1;//返回负值，则往前排
        } else if(typeA<typeB){
            LogUtil.logWithMethod(new Exception(),"nameA="+nameA+" nameB="+nameB);
            return 1;
        }


        //类型相同，需要进行进一步的比较
        int compareResult ;

        //不是字母与汉字
        if(typeA<9 && typeB<9){
            compareResult = firstStrA.compareTo(firstStrB);
            if(compareResult!=0){
                //若不同，立即出来比较结果
                LogUtil.logWithMethod(new Exception(),"nameA="+nameA+" nameB="+nameB);
                return compareResult;
            } else {
                //若相同，则递归调用
                return compareString(nameA.substring(1),nameB.substring(1));
            }
        }

        //是字母或汉字

        //若是首字母，先用第一个字母或拼音进行比较
        //否则，先判断字符类型
        String firstPinyinA = PinYinStringHelper.getFirstPingYin(nameA).substring(0, 1);
        String firstPinyinB = PinYinStringHelper.getFirstPingYin(nameB).substring(0, 1);
        if(compareCallNum==1) {
            compareResult = firstPinyinA.compareTo(firstPinyinB);
            if (compareResult != 0) {
                LogUtil.logWithMethod(new Exception(), "nameA=" + nameA + " nameB=" + nameB + " compareResult=" + compareResult);
                return compareResult;
            }
        }

        //若首字的第一个字母相同，或不是首字，判断原字符是汉字还是字母，汉字排在前面
        typeA = getFirstCharType2(nameA);
        typeB = getFirstCharType2(nameB);
        if(typeA>typeB){
            LogUtil.logWithMethod(new Exception(),"nameA="+nameA+" nameB="+nameB);
            return -1;
        } else if(typeA<typeB){
            LogUtil.logWithMethod(new Exception(),"nameA="+nameA+" nameB="+nameB);
            return 1;
        }

        //不是首字母，在字符类型之后判断，第一个字母或拼音进行比较
        if(compareCallNum!=1) {
            compareResult = firstPinyinA.compareTo(firstPinyinB);
            if (compareResult != 0) {
                LogUtil.logWithMethod(new Exception(), "nameA=" + nameA + " nameB=" + nameB + " compareResult=" + compareResult);
                return compareResult;
            }
        }

        if(isLetter(nameA)&&isLetter(nameB)) {
            //若是同一个字母，还要比较下大小写
            compareResult = firstStrA.compareTo(firstStrB);
            if (compareResult != 0) {
                LogUtil.logWithMethod(new Exception(),"nameA="+nameA+" nameB="+nameB+" compareResult="+compareResult);
                return compareResult;
            }
        }

        if(isHanzi(nameA)&&isHanzi(nameB)) {
            //使用姓的拼音进行比较
//            compareResult = firstPinyinA.compareTo(firstPinyinB);
            compareResult = PinYinStringHelper.getFirstPingYin(nameA).compareTo(PinYinStringHelper.getFirstPingYin(nameB));
            if (compareResult != 0) {
                LogUtil.logWithMethod(new Exception(),"nameA="+nameA+" nameB="+nameB);
                return compareResult;
            }

            //若姓的拼音相同，比较汉字是否相同
            compareResult = firstStrA.compareTo(firstStrB);
            if (compareResult != 0) {
                LogUtil.logWithMethod(new Exception(),"nameA="+nameA+" nameB="+nameB);
                return compareResult;
            }
        }
        //若相同，则进行下一个字符的比较（递归调用）
        return compareString(nameA.substring(1),nameB.substring(1));
    }


    /**
     * 读取asserts目录下的文件
     * @param fileName eg:"updatelog.txt"
     * @return 对应文件的内容
     *
     * */
    public static List<String> readFileFromAssets(Context context, String fileName)  {
        if (null == context || TextUtils.isEmpty( fileName )){
            throw new IllegalArgumentException( "bad arguments!" );
        }

        List<String> listString = new ArrayList<String>();
        AssetManager assetManager = context.getAssets();
        try {
            fileName = "names.txt";
            LogUtil.logWithMethod(new Exception(),"fileName="+fileName);
            InputStream instream = assetManager.open(fileName);;//new FileInputStream(file);
            LogUtil.logWithMethod(new Exception());
            if (instream != null)
            {
                InputStreamReader inputreader = new InputStreamReader(instream);
                BufferedReader buffreader = new BufferedReader(inputreader);
                String line;
                //分行读取
                while (( line = buffreader.readLine()) != null) {
                    listString.add(line);
                    LogUtil.logWithMethod(new Exception(), "line="+line);
                }
                instream.close();
            }
        }
        catch (java.io.FileNotFoundException e)
        {
            LogUtil.logWithMethod(new Exception(), "The File doesn't not exist.");
        }
        catch (IOException e)
        {
            LogUtil.logWithMethod(new Exception(), e.getMessage());
        }

        return listString;
    }

    private void setAdapter(List<ContentValues> list) {
    	adapter = new ListAdapter(this, list);
        personList.setAdapter(adapter);  
  
    }
    
    private class ListAdapter extends BaseAdapter {
    	 private LayoutInflater inflater;  
         private List<ContentValues> list;
    	
    	public ListAdapter(Context context, List<ContentValues> inList) {
    		this.inflater = LayoutInflater.from(context);
    		list = inList;
    		alphaIndexer = new HashMap<String, Integer>();
    		sections = new String[list.size()];
            LogUtil.logWithMethod(new Exception(),"list.size()="+list.size());

    		for (int i = 0; i < list.size(); i++) {
    			//当前汉语拼音首字母
    			String currentStr = getAlpha(list.get(i).getAsString(SORT_KEY).trim());

                if(!alphaIndexer.containsKey(currentStr)){
                    LogUtil.logWithMethod(new Exception(),"name="+currentStr+" i="+i);
                	alphaIndexer.put(currentStr, i);
                	sections[i] = currentStr;
                }
            }
    	}
    	
		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			return list.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {  
                convertView = inflater.inflate(R.layout.list_item, null);
                holder = new ViewHolder();  
                holder.alpha = (TextView) convertView.findViewById(R.id.alpha);  
                holder.name = (TextView) convertView.findViewById(R.id.name);  
                holder.number = (TextView) convertView.findViewById(R.id.number);  
                convertView.setTag(holder);  
            } else {  
                holder = (ViewHolder) convertView.getTag();  
            }  
            ContentValues cv = list.get(position);  
            holder.name.setText(cv.getAsString(NAME));
            holder.number.setText(cv.getAsString(NUMBER));
            String currentStr = getAlpha(list.get(position).getAsString(SORT_KEY).trim());
            String previewStr = (position - 1) >= 0 ? getAlpha(list.get(position - 1).getAsString(SORT_KEY).trim()) : " ";
            if (!previewStr.equals(currentStr)) {  
                holder.alpha.setVisibility(View.VISIBLE);
                holder.alpha.setText(currentStr);
            } else {  
                holder.alpha.setVisibility(View.GONE);
            }  
            return convertView;  
		}
		
		private class ViewHolder {
			TextView alpha;  
            TextView name;  
            TextView number;
		}
    	
    }

    //初始化汉语拼音首字母弹出提示框
    private void initOverlay() {
    	LayoutInflater inflater = LayoutInflater.from(this);
    	overlay = (TextView) inflater.inflate(R.layout.overlay, null);
    	overlay.setVisibility(View.INVISIBLE);
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_APPLICATION,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
						| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
				PixelFormat.TRANSLUCENT);
        LogUtil.logWithMethod(new Exception(),"to addView");
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		windowManager.addView(overlay, lp);
    }
    
    private class LetterListViewListener implements OnTouchingLetterChangedListener{

		@Override
		public void onTouchingLetterChanged(final String s) {
            LogUtil.logWithMethod(new Exception(),"s="+s);
            LogUtil.logWithMethod(new Exception(),"alphaIndexer="+alphaIndexer.toString());
			if(alphaIndexer.get(s) != null) {
				int position = alphaIndexer.get(s);
                LogUtil.logWithMethod(new Exception(),"to find letterInSourceDateList: position="+position);
                LogUtil.logWithMethod(new Exception(),"sections[position]="+sections[position]);

				personList.setSelection(position);
				overlay.setText(sections[position]);
				overlay.setVisibility(View.VISIBLE);
				handler.removeCallbacks(overlayThread);
				//延迟一秒后执行，让overlay为不可见
				handler.postDelayed(overlayThread, 1500);
			} 
		}
    	
    }
    
    //设置overlay不可见
    private class OverlayThread implements Runnable {

		@Override
		public void run() {
			overlay.setVisibility(View.GONE);
		}
    	
    }

    //获得汉语拼音首字母
    private String getAlpha0(String str) {
        LogUtil.logWithMethod(new Exception(),"str="+str);
        if (str == null) {
            return "#";
        }

        if (str.trim().length() == 0) {
            return "#";
        }

        char c = str.trim().substring(0, 1).charAt(0);
        // 正则表达式，判断首字母是否是英文字母
        Pattern pattern = Pattern.compile("^[A-Za-z]+$");
        if (pattern.matcher(c + "").matches()) {
            return (c + "").toUpperCase();
        } else {
            return "#";
        }
    }


    //获得汉语拼音首字母
    private String getAlpha(String str) {
//        LogUtil.logWithMethod(new Exception(),"str="+str);
        if (str == null) {  
            return "#";  
        }  
  
        if (str.trim().length() == 0) {  
            return "#";  
        }

        if(!isHanzi(str) && !isLetter(str)){
            return "#";
        }
  
        char c = str.trim().substring(0, 1).charAt(0);  
        // 正则表达式，判断首字母是否是英文字母  
        Pattern pattern = Pattern.compile("^[A-Za-z]+$");  
        if (pattern.matcher(c + "").matches()) {  
            return (c + "").toUpperCase();  
        } else {

            //汉字转拼音，获取拼音首字母
            String headChar = PinYinStringHelper.getHeadChar(str);
            if(headChar.length()>0) {
//                c = headChar.charAt(0);
                return headChar.substring(0,1);
            } else {
                return "#";
            }
        }  
    }

    boolean isLetter(String str){
        char c = str.charAt(0);
        // 正则表达式，判断首字母是否是英文字母
        Pattern pattern = Pattern.compile("^[A-Za-z]+$");
        if (pattern.matcher(c + "").matches()) {
            return true;
        }
        return false;
    }
    boolean isNumber(String str){
        char c = str.charAt(0);
        // 正则表达式，判断首字母是否是英文字母
        Pattern pattern = Pattern.compile("^[1-9]+$");
        if (pattern.matcher(c + "").matches()) {
            return true;
        }
        return false;
    }
    boolean isHanzi(String str){
        char c = str.charAt(0);
        // 正则表达式，判断首字母是否是英文字母
        Pattern pattern = Pattern.compile("[\\u4E00-\\u9FA5]+");
        if (pattern.matcher(c + "").matches()) {
            return true;
        }
        return false;
    }

    int getFirstCharType(String str){
        if(isHanzi(str)){
            return 10;
        } else if(isLetter(str)){
            return 10;
        } else if(isNumber(str)){
            return 8;
        } else {
            return 0;
        }
    }

    int getFirstCharType2(String str){
        if(isHanzi(str)){
            return 10;
        } else if(isLetter(str)){
            return 9;
        } else if(isNumber(str)){
            return 8;
        } else {
            return 0;
        }
    }


    @Override
    protected void onDestroy(){
        WindowManager windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        windowManager.removeViewImmediate(overlay);
        super.onDestroy();
    }


}  