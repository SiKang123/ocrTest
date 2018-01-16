package test.com.ocrtest;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.util.Stack;

/**
 * Created by Sikang on 2017/7/12.
 */

public class TesseractUtil {
    //字体库路径，必须包含tessdata文件夹
    static final String TESSBASE_PATH = Environment.getExternalStorageDirectory() + File.separator + "Download" + File.separator;
    //识别语言英文
    static final String NUMBER_LANGUAGE = "num";

    private static TesseractUtil mTesseractUtil = null;
    private float proportion = 0.5f;

    private TesseractUtil() {

    }

    public static TesseractUtil getInstance() {
        if (mTesseractUtil == null)
            synchronized (TesseractUtil.class) {
                if (mTesseractUtil == null)
                    mTesseractUtil = new TesseractUtil();
            }
        return mTesseractUtil;
    }

    /**
     * 调整阈值
     *
     * @param pro 调整比例
     */
    public int adjustThresh(float pro) {
        this.proportion += pro;

        if (proportion > 1f)
            proportion = 1f;
        if (proportion < 0)
            proportion = 0;
        return (int) (proportion * 100);
    }


    /**
     * 图片旋转
     *
     * @param tmpBitmap
     * @param degrees
     * @return
     */
    public static Bitmap rotateToDegrees(Bitmap tmpBitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setRotate(degrees);
        return Bitmap.createBitmap(tmpBitmap, 0, 0, tmpBitmap.getWidth(), tmpBitmap.getHeight(), matrix,
                true);
    }


    /**
     * 识别数字
     *
     * @param bmp      需要识别的图片
     * @param callBack 结果回调（携带一个String 参数即可）
     */

    public void scanNumber(final Bitmap bmp, final SimpleCallback callBack) {
        if (checkFontData()) {
            TessBaseAPI baseApi = new TessBaseAPI();
            //初始化OCR的字体数据，TESSBASE_PATH为路径，ENGLISH_LANGUAGE指明要用的字体库（不用加后缀）
            if (baseApi.init(TESSBASE_PATH, NUMBER_LANGUAGE)) {
                //设置识别模式
                baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);
                //设置要识别的图片
                baseApi.setImage(bmp);
                baseApi.setVariable(TessBaseAPI.VAR_SAVE_BLOB_CHOICES, TessBaseAPI.VAR_TRUE);
                //开始识别
                String result = baseApi.getUTF8Text();
                baseApi.clear();
                baseApi.end();
                bmp.recycle();
                callBack.response(result);
            }

        }
    }

    /**
     * 检查语言包
     */
    public boolean checkFontData() {
        File file = new File(TESSBASE_PATH + "/tessdata/");
        if (!file.exists())
            throw new RuntimeException("没有找到正确的字库目录 : \"" + TESSBASE_PATH + "/tessdata/\"");


        String fontPath = TESSBASE_PATH + "/tessdata/num.traineddata";
        file = new File(fontPath);
        if (!file.exists())
            throw new RuntimeException("没有找到正确的字库目文件 : \"" + TESSBASE_PATH + "/tessdata/num.traineddata\"");

        return true;
    }


    private void showImage(final Bitmap bmp, final ImageView imageView) {
        //将裁切的图片显示出来（测试用，需要为CameraView  setTag（ImageView））
        MainThread.getInstance().
                execute(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setVisibility(View.VISIBLE);
                        imageView.setImageBitmap(bmp);
                    }
                });
    }


    private final int PX_WHITE = -1;
    private final int PX_BLACK = -16777216;
    private final int PX_UNKNOW = -2;

    /**
     * 转为二值图像 并判断图像中是否可能有手机号
     *
     * @param bmp 原图bitmap
     * @return
     */
    public Bitmap catchPhoneRect(final Bitmap bmp, ImageView imageView) {
        int width = bmp.getWidth(); // 获取位图的宽
        int height = bmp.getHeight(); // 获取位图的高
        int[] pixels = new int[width * height]; // 通过位图的大小创建像素点数组
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        int left = width;
        int top = height;
        int right = 0;
        int bottom = 0;

        //计算阈值
        measureThresh(pixels, width, height);

        /**
         * 二值化
         * */
        binarization(pixels, width, height);
        int space = 0;
        int textWidth = 0;
        int startX = 0;
        int centerY = height / 2 - 1;
        int textLength = 0;
        int textStartX = 0;

        /**
         * 遍历中间一行像素，粗略捕捉手机号
         * */
        for (int j = 0; j < width; j++) {
            int gray = pixels[width * centerY + j];
            pixels[width * centerY + j] = getColor(gray);
            if (pixels[width * centerY + j] == PX_WHITE) {
                if (space == 1)
                    textLength++;
                if (textWidth > 0 && startX > 0 && startX < height - 1 && (space > width / 10 || j == width - 1)) {
                    if (textLength > 10 && textLength < 22)
                        if (textWidth > right - left) {
                            left = j - space - textWidth - (space / 2);
                            if (left < 0)
                                left = 0;

                            right = j - 1 - (space / 2);
                            if (right > width)
                                right = width - 1;
                            textStartX = startX;
                        }
                    textLength = 0;
                    space = 0;
                    startX = 0;
                }
                space++;
            } else {
//                pixels[width * centerY + j] = PX_BLACK;
                if (startX == 0)
                    startX = j;
                textWidth = j - startX;
                space = 0;
            }

        }

        if (right - left < width * 0.3f) {
            if (imageView != null ) {
                bmp.setPixels(pixels, 0, width, 0, 0, width, height);
                //将裁切的图片显示出来
                showImage(bmp, imageView);
            } else
            bmp.recycle();
            return null;
        }


        //粗略计算高度
        top = (int) (centerY - (right - left) / 11 * 1.5);
        bottom = (int) (centerY + (right - left) / 11 * 1.5);
        if (top < 0)
            top = 0;
        if (bottom > height)
            bottom = height - 1;

        /**
         * 判断区域中有几个字符
         * */
        //已经使用过的像素标记
        int[] usedPixels = new int[width * height];
        int[] textRect = new int[]{right, bottom, 0, 0};
        //当前捕捉文字的rect
        int[] charRect = new int[]{textStartX, centerY, 0, centerY};
        //在文字块中捕捉到的字符个数
        int charCount = 0;
        //是否发现干扰
        boolean hasStain = false;
        startX = left;
        int charMaxWidth = (right - left) / 11;
        int charMaxHeight = (int) ((right - left) / 11 * 1.5);
        boolean isInterfereCleared = false;
        while (true) {
            boolean isNormal = false;
            if (!isInterfereCleared)
                isNormal = catchCharRect(pixels, usedPixels, charRect, width, height, charMaxWidth, charMaxHeight, charRect[0], charRect[1]);
            else
                isNormal = clearInterfere(pixels, usedPixels, charRect, width, height, charMaxWidth, charMaxHeight, charRect[0], charRect[1]);
            charCount++;

            if (!isNormal) {
                hasStain = true;
            } else {
                if (hasStain && !isInterfereCleared) {
                    usedPixels = new int[width * height];
                    charMaxHeight = charRect[3] - charRect[1];
                    charMaxWidth = (int) (charMaxHeight * 0.6f);
                    charRect = new int[]{textStartX, centerY, 0, centerY};
                    charCount = 0;
                    isInterfereCleared = true;
                    continue;
                } else {
                    if (textRect[0] > charRect[0])
                        textRect[0] = charRect[0];

                    if (textRect[1] > charRect[1])
                        textRect[1] = charRect[1];

                    if (textRect[2] < charRect[2])
                        textRect[2] = charRect[2];

                    if (textRect[3] < charRect[3])
                        textRect[3] = charRect[3];

                }
            }

            boolean isFoundChar = false;
            if (!hasStain || isInterfereCleared) {
                //获取下一个字符的rect
                for (int x = charRect[2] + 1; x <= right; x++)
                    if (pixels[width * centerY + x] != PX_WHITE) {
                        isFoundChar = true;
                        charRect[0] = x;
                        charRect[1] = centerY;
                        charRect[2] = 0;
                        charRect[3] = 0;
                        break;
                    }
            } else {
                for (int x = left; x <= right; x++)
                    if (pixels[width * centerY + x] != PX_WHITE && pixels[width * centerY + x - 1] == PX_WHITE) {
                        if (x <= startX)
                            continue;
                        startX = x;
                        isFoundChar = true;
                        charRect[0] = x;
                        charRect[1] = centerY;
                        charRect[2] = x;
                        charRect[3] = centerY;
                        break;
                    }
            }
            if (!isFoundChar) {
                break;
            }
        }


        left = textRect[0];
        top = textRect[1];
        right = textRect[2];
        bottom = textRect[3];

        if (bottom - top > (right - left) / 5 || bottom - top == 0 || charCount != 11) {
            if (imageView != null ) {
                bmp.setPixels(pixels, 0, width, 0, 0, width, height);
                //将裁切的图片显示出来
                showImage(bmp, imageView);
            } else
            bmp.recycle();
            return null;
        }


        /**
         * 将最终捕捉到的手机号区域像素提取到新的数组
         * */
        int targetWidth = right - left;
        int targetHeight = bottom - top;
        int[] targetPixels = new int[targetWidth * targetHeight];
        int index = 0;

        for (int i = top; i < bottom; i++) {
            for (int j = left; j < right; j++) {
                if (index < targetPixels.length) {
                    if (pixels[width * i + j] == PX_WHITE)
                        targetPixels[index] = PX_WHITE;
                    else
                        targetPixels[index] = PX_BLACK;
                }
                index++;
            }
        }

        bmp.recycle();
        // 新建图片
        final Bitmap newBmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        newBmp.setPixels(targetPixels, 0, targetWidth, 0, 0, targetWidth, targetHeight);
        //将裁切的图片显示出来
        if (imageView != null)
            showImage(newBmp, imageView);

        return newBmp;
    }


    private final int MOVE_LEFT = 0;
    private final int MOVE_TOP = 1;
    private final int MOVE_RIGHT = 2;
    private final int MOVE_BOTTOM = 3;

    /**
     * 捕捉字符
     */
    private boolean catchCharRect(int[] pixels, int[] used, int[] charRect, int width, int height, int maxWidth, int maxHeight, int x, int y) {
        int nowX = x;
        int nowY = y;
        //记录动作
        Stack<Integer> stepStack = new Stack<>();

        while (true) {
            if (used[width * nowY + nowX] == 0) {
                used[width * nowY + nowX] = -1;
                if (charRect[0] > nowX)
                    charRect[0] = nowX;

                if (charRect[1] > nowY)
                    charRect[1] = nowY;

                if (charRect[2] < nowX)
                    charRect[2] = nowX;

                if (charRect[3] < nowY)
                    charRect[3] = nowY;

                if (charRect[2] - charRect[0] > maxWidth) {
                    return false;
                }

                if (charRect[3] - charRect[1] > maxHeight) {
                    return false;
                }

                if (nowX == 0 || nowX >= width - 1 || nowY == 0 || nowY >= height - 1) {
                    return false;
                }


            }

            //当前像素的左边是否还有黑色像素点
            int leftX = nowX - 1;
            if (leftX >= 0 && pixels[width * nowY + leftX] != PX_WHITE && used[width * nowY + leftX] == 0) {
                nowX = leftX;
                stepStack.push(MOVE_LEFT);
                continue;
            }

            //当前像素的上边是否还有黑色像素点
            int topY = nowY - 1;
            if (topY >= 0 && pixels[width * topY + nowX] != PX_WHITE && used[width * topY + nowX] == 0) {
                nowY = topY;
                stepStack.push(MOVE_TOP);
                continue;
            }


            //当前像素的右边是否还有黑色像素点
            int rightX = nowX + 1;
            if (rightX < width && pixels[width * nowY + rightX] != PX_WHITE && used[width * nowY + rightX] == 0) {
                nowX = rightX;
                stepStack.push(MOVE_RIGHT);
                continue;
            }


            //当前像素的下边是否还有黑色像素点
            int bottomY = nowY + 1;
            if (bottomY < height && pixels[width * bottomY + nowX] != PX_WHITE && used[width * bottomY + nowX] == 0) {
                nowY = bottomY;
                stepStack.push(MOVE_BOTTOM);
                continue;
            }

            if (stepStack.size() > 0) {
                int step = stepStack.pop();
                switch (step) {
                    case MOVE_LEFT:
                        nowX++;
                        break;
                    case MOVE_RIGHT:
                        nowX--;
                        break;
                    case MOVE_TOP:
                        nowY++;
                        break;
                    case MOVE_BOTTOM:
                        nowY--;
                        break;
                }
            } else {
                break;
            }
        }
        if (charRect[2] - charRect[0] == 0 || charRect[3] - charRect[1] == 0) {
            return false;
        }
        return true;
    }


    /**
     * 清除干扰
     */
    private boolean clearInterfere(int[] pixels, int[] used, int[] charRect, int width, int height, int maxWidth, int maxHeight, int x, int y) {
        int nowX = x;
        int nowY = y;
        //记录动作
        Stack<Integer> stepStack = new Stack<>();

        while (true) {
            if (used[width * nowY + nowX] == 0) {
                used[width * nowY + nowX] = -1;

                if (charRect[2] - charRect[0] < maxWidth && charRect[3] - charRect[1] < maxHeight) {
                    if (charRect[0] > nowX)
                        charRect[0] = nowX;

                    if (charRect[1] > nowY)
                        charRect[1] = nowY;

                    if (charRect[2] < nowX)
                        charRect[2] = nowX;

                    if (charRect[3] < nowY)
                        charRect[3] = nowY;
                } else {
                    pixels[width * nowY + nowX] = PX_UNKNOW;
                }
            } else if (pixels[width * nowY + nowX] == PX_UNKNOW) {
                if (charRect[2] - charRect[0] < maxWidth && charRect[3] - charRect[1] < maxHeight) {
                    pixels[width * nowY + nowX] = PX_BLACK;
                    if (charRect[0] > nowX)
                        charRect[0] = nowX;

                    if (charRect[1] > nowY)
                        charRect[1] = nowY;

                    if (charRect[2] < nowX)
                        charRect[2] = nowX;

                    if (charRect[3] < nowY)
                        charRect[3] = nowY;

                } else {
                    return true;
                }
            }

            //当前像素的左边是否还有黑色像素点
            int leftX = nowX - 1;
            if (leftX >= 0 && pixels[width * nowY + leftX] != PX_WHITE && used[width * nowY + leftX] == 0) {
                nowX = leftX;
                stepStack.push(MOVE_LEFT);
                continue;
            }

            //当前像素的上边是否还有黑色像素点
            int topY = nowY - 1;
            if (topY >= 0 && pixels[width * topY + nowX] != PX_WHITE && used[width * topY + nowX] == 0) {
                nowY = topY;
                stepStack.push(MOVE_TOP);
                continue;
            }


            //当前像素的右边是否还有黑色像素点
            int rightX = nowX + 1;
            if (rightX < width && pixels[width * nowY + rightX] != PX_WHITE && used[width * nowY + rightX] == 0) {
                nowX = rightX;
                stepStack.push(MOVE_RIGHT);
                continue;
            }


            //当前像素的下边是否还有黑色像素点
            int bottomY = nowY + 1;
            if (bottomY < height && pixels[width * bottomY + nowX] != PX_WHITE && used[width * bottomY + nowX] == 0) {
                nowY = bottomY;
                stepStack.push(MOVE_BOTTOM);
                continue;
            }

            if (stepStack.size() > 0) {
                int step = stepStack.pop();
                switch (step) {
                    case MOVE_LEFT:
                        nowX++;
                        break;
                    case MOVE_RIGHT:
                        nowX--;
                        break;
                    case MOVE_TOP:
                        nowY++;
                        break;
                    case MOVE_BOTTOM:
                        nowY--;
                        break;
                }
            } else {
                break;
            }
        }
        if (charRect[2] - charRect[0] == 0 || charRect[3] - charRect[1] == 0) {
            return false;
        }
        return true;
    }

    private int redThresh = 130;
    private int blueThresh = 130;
    private int greenThresh = 130;


    /**
     * 计算扫描线所在像素行的平均阈值
     */
    private void measureThresh(int[] pixels, int width, int height) {
        int centerY = height / 2;

        int redSum = 0;
        int blueSum = 0;
        int greenSum = 0;
        for (int j = 0; j < width; j++) {
            int gray = pixels[width * centerY + j];
            redSum += ((gray & 0x00FF0000) >> 16);
            blueSum += ((gray & 0x0000FF00) >> 8);
            greenSum += (gray & 0x000000FF);
        }

        redThresh = (int) (redSum / width * 1.5f * proportion);
        blueThresh = (int) (blueSum / width * 1.5f * proportion);
        greenThresh = (int) (greenSum / width * 1.5f * proportion);
    }

    /**
     * 二值化
     */
    private void binarization(int[] pixels, int width, int height) {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int gray = pixels[width * i + j];
                pixels[width * i + j] = getColor(gray);
                if (pixels[width * i + j] != PX_WHITE)
                    pixels[width * i + j] = PX_BLACK;
            }
        }

    }

    /**
     * 获取颜色
     */
    private int getColor(int gray) {
        int alpha = 0xFF << 24;
        // 分离三原色
        alpha = ((gray & 0xFF000000) >> 24);
        int red = ((gray & 0x00FF0000) >> 16);
        int green = ((gray & 0x0000FF00) >> 8);
        int blue = (gray & 0x000000FF);
        if (red > redThresh) {
            red = 255;
        } else {
            red = 0;
        }
        if (blue > blueThresh) {
            blue = 255;
        } else {
            blue = 0;
        }
        if (green > greenThresh) {
            green = 255;
        } else {
            green = 0;
        }
        return alpha << 24 | red << 16 | green << 8
                | blue;
    }


}
