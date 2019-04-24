#include "native-lib.h"

string receiveData="";
int mang[2];

jbyte symbol = 0;

int dem = 0;
vector<jbyte> fixErrorArray;
int kiemtra = 0;

string s1="";
string s2;

int first = 0;
int second;

int T1 = 12;
int T2 = 16;
//int T3 = 20;
int T3 = 23;
int T4 = 25;
int T5 = 33;
int T6 = 38;
//int T6 = 41;
int T7 = 42;
int T8 = 47;
int Tc = 29;

int T1d = T1-1;
int T2d = T2-1;
int T3d = T3-1;
int T4d = T4-1;
int T5d = T5-1;
int T6d = T6-1;
int T7d = T7-1;
int T8d = T8-1;
int Tcd = Tc-1;

int T1t = T1+1;
int T2t = T2+1;
int T3t = T3+1;
int T4t = T4+1;
int T5t = T5+1;
int T6t = T6+1;
int T7t = T7+1;
int T8t = T8+1;
int Tct = Tc+1;


JNIEXPORT jint JNICALL Java_com_company_cpp_hellocv_NativeClass_convertGray
        (JNIEnv *, jclass, jlong addrRgba, jlong addrGray) {
    Mat& mRgba = *(Mat*)addrRgba;
    Mat& mGray = *(Mat*)addrGray;

    int conv;
    jint retVal;
    conv = toGray(mRgba, mGray);

    retVal = (jint)conv;

    return retVal;
}

JNIEXPORT void JNICALL Java_com_company_cpp_hellocv_NativeClass_lightDetection
        (JNIEnv *, jclass, jlong addrRgba) {
    Mat& frame = *(Mat*)addrRgba;
    detectLight(frame);
}

JNIEXPORT jstring JNICALL Java_com_company_cpp_hellocv_NativeClass_getJniStringBytes
        (JNIEnv* env, jclass){
    if (dem == 5) {
        if ((symbol >= 0) && (symbol <= 127)) {
            unsigned char kytu;
            kytu =(unsigned char)symbol;
            receiveData += kytu;
            dem = 1;
            symbol = 0;
            int kichthuoc = receiveData.length();
            if (kichthuoc <= 33) {
                return (*env).NewStringUTF(receiveData.c_str());
            }
            else {
                receiveData = "";
                receiveData += kytu;
                return (*env).NewStringUTF(receiveData.c_str());
            }
        }
        else {
            return (*env).NewStringUTF("ERROR");
        }
    }   else  {
        return (*env).NewStringUTF(receiveData.c_str());
    }
}

bool kiemTra(string so1, string so2) {
    bool result;
    if (so1 == so2) {
        result = false;
    }
    else {
        result = true;
    }
    return result;
}

void detectLight(Mat& frame) {
    Mat gray;
    cvtColor(frame, gray, COLOR_BGR2GRAY);
    // Convert image to binary
    Mat bw;
    threshold(gray, bw, 80, 255, CV_THRESH_BINARY | CV_THRESH_OTSU);
    // Find all the contours in the thresholded image
    vector<Vec4i> hierarchy;
    vector<vector<Point> > contours;
    findContours(bw, contours, hierarchy, CV_RETR_LIST, CV_CHAIN_APPROX_NONE);

    vector<int> myvector;

    for (int i = 0; i < contours.size(); i++) {
        // Calculate the area of each contour
        double area = contourArea(contours[i]);
        // Ignore contours that are too small or too large
        if ((area > 1e2) && (1e6 > area))
        {
            myvector.push_back(i);
        }
    }

    /// Get the moments
    vector<Moments> mu(myvector.size());
    for( int i = 0; i < myvector.size(); i++ )
    {
        mu[i] = moments( contours[myvector[i]], false );
    }

    int hypotenuse2;
//    double chuvi;
    int kichthuocchuoi;
    int kichthuocchuoi1;

    //string distance;
    stringstream ss3;
    stringstream ss2;

    int tachmang;

    int mangthutu[myvector.size()] ;
    bool mangkiemtra[myvector.size()];

    ///  Get the mass centers:
    vector<Point2f> mc(myvector.size());
    for( int i = 0; i < myvector.size(); i++ )
    {
        mc[i] = Point2f( mu[i].m10/mu[i].m00 , mu[i].m01/mu[i].m00 );
    }


    for (int i=0; i<myvector.size(); i++) {
        for (int j = i + 1; j < myvector.size(); j++)
//            if ((mc[j].y < mc[i].y) && (abs(mc[j].x - mc[i].x) < 40 )) {
            if ( (mc[j].y < mc[i].y)) {
                int temp = myvector[i];
                myvector[i] = myvector[j];
                myvector[j] = temp;

                Point2f tam = mc[i];
                mc[i] = mc[j];
                mc[j] = tam;
            }

    }

    drawContours(frame, contours, myvector[3], Scalar(0, 255, 0), 3, 8, hierarchy, 0);
    drawContours(frame, contours, myvector[2], Scalar(0, 0, 255), 3, 8, hierarchy, 0);

    line(frame, mc[2], mc[3], Scalar(255, 0, 0), 3, CV_AA);

    if ( myvector.size() > 5 ) {
        mang[0] = (int) sqrt((double) (mc[3].y - mc[5].y) * (mc[3].y - mc[5].y) +
                             (mc[3].x - mc[5].x) * (mc[3].x - mc[5].x));
        mang[1] = (int) sqrt((double) (mc[myvector.size()-3].y - mc[myvector.size()-5].y) * (mc[myvector.size()-3].y - mc[myvector.size()-5].y) +
                             (mc[myvector.size()-3].x - mc[myvector.size()-5].x) * (mc[myvector.size()-3].x - mc[myvector.size()-5].x));
    }
    else {
        mang[0] = (int) sqrt((double) (mc[2].y - mc[4].y) * (mc[2].y - mc[4].y) +
                             (mc[2].x - mc[4].x) * (mc[2].x - mc[4].x));
        mang[1] = (int) sqrt((double) (mc[myvector.size()-2].y - mc[myvector.size()-4].y) * (mc[myvector.size()-2].y - mc[myvector.size()-4].y) +
                             (mc[myvector.size()-2].x - mc[myvector.size()-4].x) * (mc[myvector.size()-2].x - mc[myvector.size()-4].x));
    }

    ss2 << mang[1];
    std::string s2 = ss2.str();
    putText(frame, "distance:" + s2, Point2f(50, 50) , FONT_HERSHEY_PLAIN, 3,  Scalar(250,255,0), 2);

    if (dem == 0) {
        if ((mang[0] >= Tc) && (mang[0] <= Tc)) {
            dem = dem + 1;
        }
        else {
            if ((mang[1] >= Tcd) && (mang[1] <= Tct)) {
                dem = dem + 1;
            } else {
                dem = dem;
            }
        }
    }

    else if (dem==1) {
        tinhKhoangCach(mang[0], mang[1]);
    }

    else if (dem==2) {
        tinhKhoangCach(mang[0], mang[1]);
    }

    else if (dem==3) {
        tinhKhoangCach(mang[0], mang[1]);
    }

    else if (dem==4) {
        tinhKhoangCach(mang[0], mang[1]);
    }

    else {
        dem = 1;
    }
}

void tinhKhoangCach(int distance1, int distance2) {
    if ((distance1 >= T1) && (distance1 <= T1)) {
        s2 = "1";
    }

    else if ((distance1 >= T2) && (distance1 <= T2)) {
        s2 = "2";
    }

    else if ((distance1 >= T3) && (distance1 <= T3)) {
        s2 = "3";
    }

    else if ((distance1 >= T4) && (distance1 <= T4)) {
        s2 = "4";
    }

    else if ((distance1 >= T5) && (distance1 <= T5)) {
        s2 = "5";
    }

    else if ((distance1 >= T6) && (distance1 <= T6)) {
        s2 = "6";
    }

    else if ((distance1 >= T7) && (distance1 <= T7)) {
        s2 = "7";
    }

    else if ((distance1 >= T8) && (distance1 <= T8)) {
        s2 = "8";
    }

    else if ((distance1 >= Tc) && (distance1 <= Tc)) {
        s2 = "aa";
    }

    else {
        if ((distance2 >= T1d) && (distance2 <= T1t)) {
            s2 = "1";
        }

        else if ((distance2 >= T2d) && (distance2 <= T2t)) {
            s2 = "2";
        }

        else if ((distance2 >= T3d) && (distance2 <= T3t)) {
            s2 = "3";
        }

        else if ((distance2 >= T4d) && (distance2 <= T4t)) {
            s2 = "4";
        }

        else if ((distance2 >= T5d) && (distance2 <= T5t)) {
            s2 = "5";
        }

        else if ((distance2 >= T6d) && (distance2 <= T6t)) {
            s2 = "6";
        }

        else if ((distance2 >= T7d) && (distance2 <= T7t)) {
            s2 = "7";
        }

        else if ((distance2 >= T8d) && (distance2 <= T8t)) {
            s2 = "8";
        }

        else {
            s2 = "aa";
        }
    }
    int index = 64 / pow(4.0, dem - 1);

    bool check = kiemTra(s1, s2);
    if (check == true) {
        if ((s2 == "1") || (s2 == "5")) {
            symbol = symbol + 0;
            dem = dem + 1;
        }
        else if ((s2 == "2") || (s2 == "6")) {
            symbol = symbol + index;
            dem = dem + 1;
        }
        else if ((s2 == "3") || (s2 == "7")) {
            symbol = symbol + 2*index;
            dem = dem + 1;
        }
        else if ((s2 == "4") || (s2 == "8")) {
            symbol = symbol + 3*index;
            dem = dem + 1;
        }
        else {
            dem = 1;
            symbol = 0;
        }
    }
    else {
        dem = dem;
    }
    s1 = s2;
}



int toGray(Mat img, Mat& gray1) {

//    Mat imgGray, grayBlur, canny_output, grayThreshold;

//    GaussianBlur(img, imgGray, Size(3,3), 0, 0);

    // Set threshold and maxValue
//    double threshBlur = 90;
//    double maxValue = 255;

    // Binary Threshold
//    threshold(imgGray,grayThreshold, threshBlur, maxValue, THRESH_BINARY);


    // Convert image to grayscale
    Mat gray;
    cvtColor(img, gray, COLOR_BGR2GRAY);
    // Convert image to binary
    Mat bw;
    threshold(gray, bw, 50, 255, CV_THRESH_BINARY | CV_THRESH_OTSU);
    // Find all the contours in the thresholded image
    vector<Vec4i> hierarchy;
    vector<vector<Point> > contours;
    findContours(bw, contours, hierarchy, CV_RETR_LIST, CV_CHAIN_APPROX_NONE);

    double maxAreaContour = 1e3;
    int giaTriCuaMaxContour = 0;


    for (size_t i = 0; i < contours.size(); ++i) {
        // Calculate the area of each contour
        double area = contourArea(contours[i]);
        // Ignore contours that are too small or too large
        if (area < 1e3 || 1e4 < area) continue;

        if (maxAreaContour < area) {
            giaTriCuaMaxContour = (int) i;
            maxAreaContour = area;
        }
    }
    getOrientation(contours[giaTriCuaMaxContour], img);

    gray1 = img;

//    if(gray1.rows == img.rows && gray.cols == img.cols)
    return 1;
//    return 0;

}

void drawAxis(Mat& img, Point p, Point q, Scalar colour, const float scale = 0.2)
{
    double angle;
    double hypotenuse;
    angle = atan2( (double) p.y - q.y, (double) p.x - q.x ); // angle in radians
    hypotenuse = sqrt( (double) (p.y - q.y) * (p.y - q.y) + (p.x - q.x) * (p.x - q.x)) + 300;
//    double degrees = angle * 180 / CV_PI; // convert radians to degrees (0-180 range)
//    cout << "Degrees: " << abs(degrees - 180) << endl; // angle in 0-360 degrees range
    // Here we lengthen the arrow by a factor of scale
    q.x = (int) (p.x - scale * hypotenuse * cos(angle));
    q.y = (int) (p.y - scale * hypotenuse * sin(angle));
    p.x = p.x - 300;
    p.y = p.y;
    line(img, p, q, colour, 3, CV_AA);
//    // create the arrow hooks
//    p.x = (int) (q.x + 9 * cos(angle + CV_PI / 4));
//    p.y = (int) (q.y + 9 * sin(angle + CV_PI / 4));
//    line(img, p, q, colour, 1, CV_AA);
//    p.x = (int) (q.x + 9 * cos(angle - CV_PI / 4));
//    p.y = (int) (q.y + 9 * sin(angle - CV_PI / 4));
//    line(img, p, q, colour, 1, CV_AA);
}
double getOrientation(const vector<Point> &pts, Mat &img)
{
    //Construct a buffer used by the pca analysis
    int sz = static_cast<int>(pts.size());
    Mat data_pts = Mat(sz, 2, CV_64FC1);
    for (int i = 0; i < data_pts.rows; ++i)
    {
        data_pts.at<double>(i, 0) = pts[i].x;
        data_pts.at<double>(i, 1) = pts[i].y;
    }
    //Perform PCA analysis
    PCA pca_analysis(data_pts, Mat(), CV_PCA_DATA_AS_ROW);
    //Store the center of the object
    Point cntr = Point(static_cast<int>(pca_analysis.mean.at<double>(0, 0)),
                       static_cast<int>(pca_analysis.mean.at<double>(0, 1)));
    //Store the eigenvalues and eigenvectors
    vector<Point2d> eigen_vecs(2);
    vector<double> eigen_val(2);
    for (int i = 0; i < 2; ++i)
    {
        eigen_vecs[i] = Point2d(pca_analysis.eigenvectors.at<double>(i, 0),
                                pca_analysis.eigenvectors.at<double>(i, 1));
        eigen_val[i] = pca_analysis.eigenvalues.at<double>(0, i);
    }
    // Draw the principal components
//    circle(img, cntr, 3, Scalar(255, 0, 255), 2);
    Point p1 = cntr + 0.02 * Point(static_cast<int>(eigen_vecs[0].x * eigen_val[0]), static_cast<int>(eigen_vecs[0].y * eigen_val[0]));
//    Point p2 = cntr - 0.02 * Point(static_cast<int>(eigen_vecs[1].x * eigen_val[1]), static_cast<int>(eigen_vecs[1].y * eigen_val[1]));
    drawAxis(img, cntr, p1, Scalar(0, 255, 0), 1);
//    drawAxis(img, cntr, p2, Scalar(255, 255, 0), 5);
    double angle = atan2(eigen_vecs[0].y, eigen_vecs[0].x); // orientation in radians
    return angle;
}