package com.example.jolin.afinal;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by Administrator on 2017/2/15 0015.//自定义相机
 */
public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.AutoFocusCallback {

    private static final String TAG = "CameraSurfaceView";

    private Context mContext;
    private SurfaceHolder holder;
    private Camera mCamera;

    private MainActivity mmain;
    private int mScreenWidth;
    private int mScreenHeight;
    private CameraTopRectView topView;

    private FaceDetect faceDetect = new FaceDetect();
    //更動
    private String filePath;
    private Activity activity;

    private File file;

    private static byte[] byteFile = null;
    private static int byteCount;

    public CameraSurfaceView(Context context) {
        this(context, null);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        getScreenMetrix(context);
        topView = new CameraTopRectView(context, attrs);

        initView();
    }

    //拿到手机屏幕大小
    private void getScreenMetrix(Context context) {
        WindowManager WM = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        WM.getDefaultDisplay().getMetrics(outMetrics);
        mScreenWidth = outMetrics.widthPixels;
        mScreenHeight = outMetrics.heightPixels;
    }

    private void initView() {
        holder = getHolder();//获得surfaceHolder引用
        holder.addCallback(this);
//        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);//设置类型
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");
        if (mCamera == null) {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);//开启相机
            try {
                mCamera.setPreviewDisplay(holder);//摄像头画面显示在Surface上
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged");

        setCameraParams(mCamera, mScreenWidth, mScreenHeight);
        mCamera.startPreview();
//        mCamera.takePicture(null, null, jpeg);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
        mCamera.stopPreview();//停止预览
        mCamera.release();//释放相机资源
        mCamera = null;
        holder = null;
    }

    @Override
    public void onAutoFocus(boolean success, Camera Camera) {
        if (success) {
            Log.i(TAG, "onAutoFocus success=" + success);
            System.out.println(success);
        }
    }

    private void setCameraParams(Camera camera, int width, int height) {
        Log.i(TAG, "setCameraParams  width=" + width + "  height=" + height);
        Camera.Parameters parameters = mCamera.getParameters();
        // 获取摄像头支持的PictureSize列表
        List<Camera.Size> pictureSizeList = parameters.getSupportedPictureSizes();
        for (Camera.Size size : pictureSizeList) {
            Log.i(TAG, "pictureSizeList size.width=" + size.width + "  size.height=" + size.height);
        }
        /**从列表中选取合适的分辨率*/
        Camera.Size picSize = getProperSize(pictureSizeList, ((float) height / width));
        if (null == picSize) {
            Log.i(TAG, "null == picSize");
            picSize = parameters.getPictureSize();
        }
        Log.i(TAG, "picSize.width=" + picSize.width + "  picSize.height=" + picSize.height);
        // 根据选出的PictureSize重新设置SurfaceView大小
        float w = picSize.width;
        float h = picSize.height;
        parameters.setPictureSize(picSize.width, picSize.height);
        this.setLayoutParams(new FrameLayout.LayoutParams((int) (height * (h / w)), height));

        // 获取摄像头支持的PreviewSize列表
        List<Camera.Size> previewSizeList = parameters.getSupportedPreviewSizes();

        for (Camera.Size size : previewSizeList) {
            Log.i(TAG, "previewSizeList size.width=" + size.width + "  size.height=" + size.height);
        }
        Camera.Size preSize = getProperSize(previewSizeList, ((float) height) / width);
        if (null != preSize) {
            Log.i(TAG, "preSize.width=" + preSize.width + "  preSize.height=" + preSize.height);
            parameters.setPreviewSize(preSize.width, preSize.height);
        }

        parameters.setJpegQuality(100); // 设置照片质量
        if (parameters.getSupportedFocusModes().contains(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 连续对焦模式
        }

        mCamera.cancelAutoFocus();//自动对焦。
        mCamera.setDisplayOrientation(90);// 设置PreviewDisplay的方向，效果就是将捕获的画面旋转多少度显示
        mCamera.setParameters(parameters);
    }

    /**
     * 从列表中选取合适的分辨率
     * 默认w:h = 4:3
     * <p>注意：这里的w对应屏幕的height
     * h对应屏幕的width<p/>
     */
    private Camera.Size getProperSize(List<Camera.Size> pictureSizeList, float screenRatio) {
        Log.i(TAG, "screenRatio=" + screenRatio);
        Camera.Size result = null;
        for (Camera.Size size : pictureSizeList) {
            float currentRatio = ((float) size.width) / size.height;
            if (currentRatio - screenRatio == 0) {
                result = size;
                break;
            }
        }

        if (null == result) {
            for (Camera.Size size : pictureSizeList) {
                float curRatio = ((float) size.width) / size.height;
                if (curRatio == 4f / 3) {// 默认w:h = 4:3
                    result = size;
                    break;
                }
            }
        }
        return result;
    }


    // 拍照瞬间调用
    private Camera.ShutterCallback shutter = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            Log.i(TAG, "shutter");
            System.out.println("执行了吗+1");
        }
    };

    // 获得没有压缩过的图片数据
    private Camera.PictureCallback raw = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera Camera) {
            Log.i(TAG, "raw");
            System.out.println("执行了吗+2");
        }
    };

    //创建jpeg图片回调数据对象
    private Camera.PictureCallback jpeg = new Camera.PictureCallback() {

        private Bitmap bitmap;

        @Override
        public void onPictureTaken(byte[] data, Camera Camera) {
            topView.draw(new Canvas());

            BufferedOutputStream bos = null;
            ByteArrayOutputStream baos = null;
            Bitmap bm = null;
            if (data != null) {
            }

            try {
                // 获得图片
                bm = BitmapFactory.decodeByteArray(data, 0, data.length);
                Log.d("checkpoint", "checkpoint - " + bm);
//                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//                    String filePath = "/sdcard/dyk" + System.currentTimeMillis() + ".JPEG";//照片保存路径

//                    //图片存储前旋转
                Matrix m = new Matrix();
                int height = bm.getHeight();
                int width = bm.getWidth();
                m.setRotate(-90);
                //旋转后的图片

                Bitmap iconbitmap = BitmapFactory.decodeResource(getResources() , R.drawable.light);

                bitmap = Bitmap.createBitmap(bm, 0, 0, width, height, m, true);

                //bitmap = bitmap.copy(Bitmap.Config.RGB_565, true);



                //Bitmap editbitmap = toeditBitmap(bitmap);
                Bitmap editbitmap = toConformBitmap(bitmap,iconbitmap);
                // faceDetect.start(bitmap);
                // new SkinDetect(bitmap);
                // SkinDetect.RGBskinDetection(bitmap);


                System.out.println("执行了吗+3");
                file = new File(filePath);
                if (!file.exists()) {
                    file.createNewFile();
                }
                // bos = new BufferedOutputStream(new FileOutputStream(file));
                baos = new ByteArrayOutputStream();

                Bitmap sizeBitmap = Bitmap.createScaledBitmap(editbitmap,
                        topView.getViewWidth(), topView.getViewHeight(), true);
                bm = Bitmap.createBitmap(sizeBitmap);// 截取

                // bm.compress(Bitmap.CompressFormat.JPEG, 70, bos);//将图片压缩到流中
                bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                //70 是压缩率，表示压缩30%; 如果不压缩是100，表示压缩率为0
                byteFile = baos.toByteArray();
                byteCount = byteFile.length;
                System.out.println("byteCount = " + byteCount);
                getByteFile();

                /*byteCount = bm.getByteCount();

                ByteBuffer buf = ByteBuffer.allocate(byteCount);
                bm.copyPixelsToBuffer(buf);

                byteFile = buf.array();*/
                // byteFile = new byte[byteCount];

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    // bos.flush();//输出
                    // bos.close();//关闭

                    baos.flush();
                    baos.close();

                    bm.recycle();// 回收bitmap空间
                    mCamera.stopPreview();// 关闭预览
                    activity.setResult(Activity.RESULT_OK);
                    activity.finish();
//                    mCamera.startPreview();// 开启预览
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public void takePicture(Activity activity, String filePath) {
        this.filePath = filePath;
        this.activity = activity;
        //设置参数,并拍照
        setCameraParams(mCamera, mScreenWidth, mScreenHeight);
        // 当调用camera.takePiture方法后，camera关闭了预览，这时需要调用startPreview()来重新开启预览
        mCamera.takePicture(null, null, jpeg);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public static int getByteCount(){
        return byteCount;
    }
    public static byte[] getByteFile(){
        Client.lock.lock();
        try{
            Client.condition.signal();
            System.out.println("Client Thread Signal!");
        }finally {
            Client.lock.unlock();
        }
        return byteFile;
    }
    private Bitmap toeditBitmap(Bitmap bitmap) throws InterruptedException {
        Resources pathname = getResources();
        Editface editface = new Editface(bitmap, pathname);
        Thread t = new Thread(editface);
        System.out.println("-------start edit ---------------");
        t.setName("Editpic");
        t.start();


        Bitmap edited = editface.getEditpic();
        return  edited;
    }

    public static Bitmap toConformBitmap(Bitmap background, Bitmap foreground) {
        System.out.println("start conform");
        if (background == null) {
            return null;
        }

        int bgWidth = background.getWidth();
        int bgHeight = background.getHeight();
        //create the new blank bitmap
        Bitmap newbmp = Bitmap.createBitmap(bgWidth, bgHeight, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(newbmp);
        //draw bg into
        cv.drawBitmap(background, 0, 0, null);//在 0，0座標開始畫入bg
        //draw fg into
        cv.drawBitmap(foreground, 0, 0, null);//從任意位置畫
        //save all clip
        cv.save(Canvas.ALL_SAVE_FLAG);//保存
        //store
        cv.restore();//存储
        return newbmp;
    }
}