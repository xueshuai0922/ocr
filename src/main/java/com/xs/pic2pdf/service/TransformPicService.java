package com.xs.pic2pdf.service;

import cn.hutool.core.codec.Base64Encoder;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.img.Img;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.itextpdf.pdfocr.OcrPdfCreator;
import com.itextpdf.pdfocr.tesseract4.Tesseract4LibOcrEngine;
import com.itextpdf.pdfocr.tesseract4.Tesseract4OcrEngineProperties;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;
import com.sun.imageio.plugins.common.ImageUtil;
import com.xs.CommonResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opencv.core.Core.circle;
import static org.opencv.core.Core.line;
import static org.opencv.imgproc.Imgproc.INTER_LINEAR;

/**
 * @author xueshuai
 * @date 2020/10/15 13:43
 * @description
 */
@Service
public class TransformPicService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());


    /**
     * opencv进行获取透视转换需要的四个坐标点，返回给页面
     *
     * @param imagPath 图片路径
     * @return
     */
    public CommonResult getPoints(String imagPath) {

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        logger.info("\n 图像转换开始");
        //我们假设我们识别的图片如样例一样有明显的边界，那我们可以用边缘检测算法将真正有效区域抽离出来，
        //以此来提高识别准确度和识别精度
        //先进行边缘检测
        String sourcePath = "C:/Users/lenovo/Desktop/3.jpg";
//		String sourcePath = Constants.PATH + imagefile;
        Mat source = Highgui.imread(imagPath);
//		Mat destination = new Mat(source.rows(), source.cols(), source.type());
        //复制一个source作为四点转换的原图，因为source在轮廓识别时会被覆盖，建议图像处理时都将原图复制一份，
        //因为opencv的很多算法都会更改传入的soure图片，如果不注意可能就会导致各种异常。
        Mat orign = source.clone();
        //为了加速图像处理，以及使我们的边缘检测步骤更加准确，我们将扫描图像的大小调整为具有500像素的高度。
        Mat dst = source.clone();
//		//缩放比例
//		double ratio = NumberUtil.div(500, orign.height());
//		System.out.println("----------"+ratio);
//		double width = ratio*orign.width();
//		Imgproc.resize(source, dst, new Size(width,500));


        // 灰度化,加载为灰度图显示
        Mat gray = dst.clone();
        Imgproc.cvtColor(dst, gray, Imgproc.COLOR_BGR2GRAY);

        //高斯滤波,去除杂点等干扰
        Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);

//        Highgui.imwrite("C:/Users/lenovo/Desktop/gray.jpg", gray);


        //canny边缘检测算法，经过canny算法或的图像会变成二值化效果
        Mat edges = gray.clone();
        Imgproc.Canny(gray, edges, 30, 120);
//        Highgui.imwrite("C:/Users/lenovo/Desktop/canny.jpg", edges);


        //【5】形态学闭操作
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3), new Point(-1, -1));
        Imgproc.morphologyEx(edges, edges, Imgproc.MORPH_CLOSE, kernel, new Point(-1, -1), 3);
        //【6】查找，筛选，绘制轮廓
        Mat hierarchy = new Mat(gray.rows(), gray.cols(), CvType.CV_8UC1, new Scalar(0));
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();


        //轮廓识别，查找外轮廓
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE, new Point());

        double width = source.cols() * 1.0;
        double height = source.rows() * 1.0;
        Mat drawImage = Mat.zeros(source.size(), CvType.CV_8UC3);
        for (int t = 0; t < contours.size(); t++) {
            Rect rect = Imgproc.boundingRect(contours.get(t));
            if (rect.width > width / 2 && rect.width < width - 5) {
                //画外轮廓边框
                Imgproc.drawContours(drawImage, contours, t, new Scalar(0, 0, 255), 2, 8, hierarchy, 0, new Point());
            }
        }


        //【7】计算直线相关信息

        Point center = new Point();//原图像正中心
        center.y = source.rows() / 2.0;
        center.x = source.cols() / 2.0;

        //霍夫线检测
        Mat contoursImg = new Mat();
        double accu = Math.min(width * 0.05, height * 0.05);
        Imgproc.cvtColor(drawImage, contoursImg, Imgproc.COLOR_BGR2GRAY);
        Mat lineMat = new Mat();

        Imgproc.HoughLinesP(contoursImg, lineMat, 1, Math.PI / 180.0, (int) accu, accu, 0);
        Mat linesImage = Mat.zeros(source.size(), CvType.CV_8UC3);
        int size = lineMat.cols();
        double[] k = new double[size];
        double[] c = new double[size];//直线斜率K，常数项C y=kx+c
        double[] length = new double[size];//距离直线的距离
        for (int i = 0; i < lineMat.cols(); i++) {
            double a = lineMat.get(0, i)[0];
            double b = lineMat.get(0, i)[1];
            double c1 = lineMat.get(0, i)[2];
            double d = lineMat.get(0, i)[3];
            line(linesImage, new Point(a, b), new Point(c1, d), new Scalar(0, 0, 255), 1, Core.LINE_AA, 0);
//			line(linesImage, new Point(a, a[i + 1]), new Point(a[i + 2], a[i + 3]), new Scalar(0, 255, 0), 2,8,0);
            if (a != c1) {//当直线不垂直时
                k[i] = (d - b) / (c1 - a);
                c[i] = b - k[i] * a;
                length[i] = Math.abs(k[i] * center.x - center.y + c[i]) / Math.sqrt((Math.pow(k[i], 2) + 1));
            } else {//直线垂直时
                k[i] = height;

                c[i] = (d - k[i] * a);
                //length[t] = abs(k[t] * center.x - center.y + c[t]) / pow((sqrt(k[t]) + 1), 0.5);
                length[i] = Math.abs(a - center.x);
            }


        }
//        Highgui.imwrite("C:/Users/lenovo/Desktop/linesImage.jpg", linesImage);


        // 【7】寻找与定位上下左右四条直线
        double t = height;
        double b = height;
        double l = width;
        double r = width;
        int t_ = 0, b_ = 0, l_ = 0, r_ = 0; //数组坐标

        double[] topLine = new double[size];
        double[] bottomLine = new double[size];
        double[] leftLine = new double[size];
        double[] rightLine = new double[size];
        double TopMax = 0.0d;
        double BottomMax = 0.0d;
        double leftMax = 0.0d;
        double rightMax = 0.0d;
        for (int i = 0; i < lineMat.cols(); i++) {//遍历所有直线
            double x1 = lineMat.get(0, i)[0];
            double y1 = lineMat.get(0, i)[1];
            double x2 = lineMat.get(0, i)[2];
            double y2 = lineMat.get(0, i)[3];

            //opencv 中以坐上为坐标原点，如果直线的y值都小于中心点，那么它就是顶部直线，同理其他直线类似
            if (y1 < height / 2.0 && y2 < height / 2.0) {//顶部直线

                if (length[i] < t && length[i] > TopMax && Math.abs(k[i]) < 1) {

                    topLine = lineMat.get(0, i);
                    TopMax = length[i];
                    t_ = i;
                }
                continue;
            }
            if (x1 < width / 2.0 && x2 < width / 2.0) {//左部直线  而且要保证是条接近的竖直线


                if (length[i] < l && length[i] > leftMax && Math.abs(k[i]) > 1) {
                    leftLine = lineMat.get(0, i);
                    leftMax = length[i];
                    l_ = i;
                }
                continue;
            }
            if (y1 > height / 2.0 && y2 > height / 2.0) {//底部直线

                if (length[i] < b && length[i] > BottomMax && Math.abs(k[i]) < 1) {
                    bottomLine = lineMat.get(0, i);
                    BottomMax = length[i];
                    b_ = i;
                }
                continue;
            }
            if ((x1 > width / 2.0) && (x2 > width / 2.0) && Math.abs(k[i]) > 1) {//右部直线

                if (length[i] < r && length[i] > rightMax) {
                    rightLine = lineMat.get(0, i);
                    rightMax = length[i];
                    r_ = i;
                }
                continue;
            }
        }


        //打印直线两个端点
        line(linesImage, new Point(topLine[0], topLine[1]), new Point(topLine[2], topLine[3]), new Scalar(100, 255, 255), 2, 8, 0);
        line(linesImage, new Point(bottomLine[0], bottomLine[1]), new Point(bottomLine[2], bottomLine[3]), new Scalar(0, 100, 255), 2, 8, 0);
        line(linesImage, new Point(leftLine[0], leftLine[1]), new Point(leftLine[2], leftLine[3]), new Scalar(255, 255, 255), 2, 8, 0);
        line(linesImage, new Point(rightLine[0], rightLine[1]), new Point(rightLine[2], rightLine[3]), new Scalar(255, 255, 255), 2, 8, 0);
//        Highgui.imwrite("C:/Users/lenovo/Desktop/point.jpg", linesImage);

        //【8】计算四个定点并透视变换
        // 四条直线交点
        Point p1 = new Point(); // 左上角
        p1.x = (int) ((c[t_] - c[l_]) / (k[l_] - k[t_]));
        p1.y = (int) (k[t_] * p1.x + c[t_]);
        Point p2 = new Point(); // 右上角
        p2.x = (int) ((c[t_] - c[r_]) / (k[r_] - k[t_]));
        p2.y = (int) (k[t_] * p2.x + c[t_]);
        Point p3 = new Point(); // 左下角
        p3.x = (int) ((c[b_] - c[l_]) / (k[l_] - k[b_]));
        p3.y = (int) (k[b_] * p3.x + c[b_]);
        Point p4 = new Point(); // 右下角
        p4.x = (int) ((c[b_] - c[r_]) / (k[r_] - k[b_]));
        p4.y = (int) (k[b_] * p4.x + c[b_]);

        ArrayList<Point> points = new ArrayList<Point>();
        points.add(p1);
        points.add(p2);
        points.add(p3);
        points.add(p4);
        return CommonResult.SUCCESS(points);


    }

    /**
     * opencv 透视转换，进行矫正图片
     * imgPath 图片路径
     * points 四个坐标点（顺序：左上，右上，左下，右下）
     *
     * @return  返回图片base64编码
     */
    public CommonResult transformPic(Map<String, Object> data) {

        CommonResult commonResult = this.handleTransform(data);
        String uniPath =commonResult.getData()+"";
        byte[] readBytes = FileUtil.readBytes(uniPath);
        // 对字节数组进行Base64编码，得到Base64编码的字符串
        String imgBase64Info = Base64Encoder.encode(readBytes);
        //把文件删除
        FileUtil.del(uniPath);
        //传回Base64编码信息
        return new CommonResult(imgBase64Info, "0", "透视转换成功");
    }


    private CommonResult handleTransform(Map<String, Object> data){
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        String imgPath = data.get("imgPath") + "";
        if (StrUtil.isEmpty(imgPath)) {
            return CommonResult.FAIL("检测参数imgPath");
        }
        List<Map<String, Integer>> pointsList = (List<Map<String, Integer>>) data.get("points");
        List<Point> points = new ArrayList<Point>();
        for (Map<String, Integer> stringMap : pointsList) {
            double x = stringMap.get("x") * 1.0;
            double y = stringMap.get("y") * 1.0;
            Point point = new Point(x, y);
            points.add(point);
        }
        Mat source = Highgui.imread(imgPath);

        double width = source.cols() * 1.0;
        double height = source.rows() * 1.0;
        Point p1 = points.get(0);//左上角
        Point p2 = points.get(1);//右上角
        Point p3 = points.get(2);//左下角
        Point p4 = points.get(3);//右下角

        circle(source, p1, 2, new Scalar(255, 225, 225), 2, 8, 0);
        circle(source, p2, 2, new Scalar(255, 225, 0), 2, 8, 0);
        circle(source, p3, 2, new Scalar(255, 0, 0), 2, 8, 0);
        circle(source, p4, 2, new Scalar(255, 0, 225), 2, 8, 0);
        // 透视变换
        MatOfPoint2f cornerMat = new MatOfPoint2f(p1, p2, p3, p4);
        MatOfPoint2f quadMat = new MatOfPoint2f(
                new Point(0, 0),
                new Point(width, 0),
                new Point(0, height),
                new Point(width, height)
        );

        // 获取透视变换矩阵
        Mat resultImage = new Mat();
        Mat warpmatrix = Imgproc.getPerspectiveTransform(cornerMat, quadMat);
        Imgproc.warpPerspective(source, resultImage, warpmatrix, resultImage.size(), INTER_LINEAR);
        //给个随机id
        String imgRandomId = IdUtil.randomUUID();
        String basePath = System.getProperty("user.dir");
        String uniPath=basePath+"\\"+imgRandomId+".jpg";
        Highgui.imwrite(uniPath, resultImage);
        return CommonResult.SUCCESS(uniPath);
    }


    public CommonResult toPdf(List<String> imgPaths, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();

        String now = DateUtil.now().replace(":", "-");
        String target = "C:/Users/lenovo/Desktop/" + now + ".pdf";
        Document document = new Document();
        //设置文档页边距
        document.setMargins(0, 0, 0, 0);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(target);
            PdfWriter.getInstance(document, fos);
            //打开文档
            document.open();
            for (String source : imgPaths) {

                String realPath = System.getProperty("user.dir");

                UUID uuid = UUID.fastUUID();
                File srcFile = new File(source);
                File destFile = new File(realPath + "/" + now + "-" + uuid + ".jpg");

//                FileUtils.copyFile(srcFile,destFile);
                //图片进行压缩,加快转换pdf
                Img.from(srcFile)
                        .setQuality(0.8)//压缩比率
                        .write(destFile);


                //获取图片的宽高
                Image image = Image.getInstance(now + "-" + uuid + ".jpg");
                float imageHeight = image.getScaledHeight();
                float imageWidth = image.getScaledWidth();

                //设置页面宽高与图片一致
                Rectangle rectangle = new Rectangle(imageWidth, imageHeight);
                document.setPageSize(rectangle);
                //图片居中
                image.setAlignment(Image.ALIGN_CENTER);
                //新建一页添加图片
                document.newPage();
                document.add(image);
                FileUtils.deleteQuietly(destFile);
            }

        } catch (Exception ioe) {
            logger.error(ioe.getMessage());
            ioe.printStackTrace();
        } finally {
            //关闭文档
            document.close();
            try {
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //将pdf弄成下载？？？？

        System.out.println("耗时" + (System.currentTimeMillis() - startTime) + "ms");
        return CommonResult.SUCCESS();
    }

    public CommonResult toSmartPdf(List<String> imgPaths, HttpServletRequest request) {
        ArrayList<String> pathsList = new ArrayList<String>();
        for (String imgPath : imgPaths) {
            CommonResult points = this.getPoints(imgPath);
            Map<String, Object> data = new HashMap<String, Object>();
            List<Point> pointList=(List<Point>)points.getData();

            ArrayList<HashMap<String, Integer>> objects = new ArrayList<HashMap<String, Integer>>();
            for (Point point:pointList){
                HashMap<String, Integer> pointsMap = new HashMap<String, Integer>();
                pointsMap.put("x",(int)point.x);
                pointsMap.put("y",(int)point.y);
                objects.add(pointsMap);
            }

            data.put("points", objects);
            data.put("imgPath", imgPath);
            CommonResult transResult = this.handleTransform(data);
            String uniPath =transResult.getData()+"";
            pathsList.add(uniPath);
        }
        CommonResult commonResult = this.toPdf(pathsList, request);
        for (String path:pathsList){
            FileUtil.del(path);
        }
        return commonResult;

    }
}
