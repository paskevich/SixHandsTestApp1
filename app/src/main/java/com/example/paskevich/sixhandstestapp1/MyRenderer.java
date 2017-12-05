package com.example.paskevich.sixhandstestapp1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_COLOR_ATTACHMENT0;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_FRAMEBUFFER_BINDING;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_RENDERBUFFER;
import static android.opengl.GLES20.GL_RENDERBUFFER_BINDING;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE1;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindRenderbuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glCopyTexImage2D;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glDrawElements;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glFramebufferTexture2D;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glGenRenderbuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetFramebufferAttachmentParameteriv;
import static android.opengl.GLES20.glGetIntegerv;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

/**
 * Created by paskevich on 25.11.17.
 */

public class MyRenderer implements GLSurfaceView.Renderer {

    public static final int POSITION_COUNT = 3;
    public static final int TEXTURE_COUNT = 2;
    public static final int STRIDE = (POSITION_COUNT + TEXTURE_COUNT) * 4;

    private Context context;

    private FboRenderer fboRenderer;

    private Bitmap bitmapImage;
    private Bitmap fboBitmap;

    private FloatBuffer vertexData;

    private int texture = -1;

    private int aPositionLocation;
    private int aTextureLocation;
    private int uTextureUnitLocation;
    private int uProjMatrixLocation;
    private int uModelMatrixLocation;
    private int uViewMatrixLocation;
    private int uImageTexLocation;
    private int uMaskTexLocation;

    private int programId;

    private float[] mProjectionMatrix = new float[16];
    private float[] mModelMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private int height;
    private int width;

    public MyRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        fboBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.innah);
        glClearColor(0f, 1f, 1f, 1f);
        Log.d("Surface: ", "surface created");
        glEnable(GL_DEPTH_TEST);

        createProgram();
        getLocations();

        fboRenderer = new FboRenderer(context);

        /*BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        bitmapImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.innah, options);*/
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        Log.d("Surface", "onSurfaceChanged: width: " + width + " height: " + height);
        //glViewport(0, 0, width, height);
        this.width = width;
        this.height = height;

        fboRenderer.fboInit(width, height);

        glViewport(0,0, width, height);

        if (bitmapImage != null) {
            //Log.d("QWERTY", "onSurfaceChanged: Image Width: " + bitmapImage.getWidth() + " Image Height: " + bitmapImage.getHeight());

            glBindFramebuffer(GL_FRAMEBUFFER, fboRenderer.getOldFbId());
            glUseProgram(programId);
            prepareData(width, height);
            bindData();
            createProjectionMatrix(width, height);
            createModelMatrix(bitmapImage.getWidth(), bitmapImage.getHeight(), width, height);
            createViewMatrix(width, height);
            bindMatrix();
        }
    }


    @Override
    public void onDrawFrame(GL10 gl10) {

        Log.d("Surface", "onDrawFrame: ");

        if (bitmapImage != null) {
            glBindFramebuffer(GL_FRAMEBUFFER, fboRenderer.getOldFbId());
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glDrawArrays(GL_TRIANGLE_STRIP, 0 ,4);
        } else {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        }
    }

    public void setBitmapImage(Bitmap bitmapImage) {
        this.bitmapImage = bitmapImage;
    }

    private void prepareData(int w, int h) {
        float[] vertices = {
                -1, 1, 1, 0, 0,
                1, 1, 1, 1, 0,
                -1, -1, 1, 0, 1,
                1, -1, 1, 1, 1,
        };
        vertexData = ByteBuffer
                .allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexData.put(vertices);

        /*if (texture != -1) {
            glDeleteTextures(1, IntBuffer.allocate(texture));
            texture = -1;
        }*/
        texture = TextureUtils.loadTexture(context, bitmapImage);
    }

    private void getLocations() {
        aPositionLocation = glGetAttribLocation(programId, "a_Position");
        aTextureLocation = glGetAttribLocation(programId, "a_Texture");
        uProjMatrixLocation = glGetUniformLocation(programId, "u_ProjMatrix");
        uModelMatrixLocation = glGetUniformLocation(programId, "u_ModelMatrix");
        uViewMatrixLocation = glGetUniformLocation(programId, "u_ViewMatrix");

        //uncomment for another shaders
        //uTextureUnitLocation = glGetUniformLocation(programId, "u_TextureUnit");

        uImageTexLocation = glGetUniformLocation(programId, "u_ImageTex");
        uMaskTexLocation = glGetUniformLocation(programId, "u_MaskTex");
    }

    private void bindData() {
        vertexData.position(0);
        glEnableVertexAttribArray(aPositionLocation);
        glVertexAttribPointer(aPositionLocation, POSITION_COUNT, GL_FLOAT, false, STRIDE, vertexData);

        vertexData.position(POSITION_COUNT);
        glEnableVertexAttribArray(aTextureLocation);
        glVertexAttribPointer(aTextureLocation, TEXTURE_COUNT, GL_FLOAT, false, STRIDE, vertexData);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);

        Log.d("WHAT HAPPENS: ", "TEXTURE WAS BINDED");

        //uncomment for another shaders
        //glUniform1i(uTextureUnitLocation, 0);

        glUniform1i(uImageTexLocation, 0);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, fboRenderer.getFboTex());
        glUniform1i(uMaskTexLocation, 1);
    }

    private void createProgram() {
        /*int vertexShaderId = ShaderUtils.createShader(context, GL_VERTEX_SHADER, R.raw.vertex_shader);
        int fragmentShaderId = ShaderUtils.createShader(context, GL_FRAGMENT_SHADER, R.raw.fragment_shader);*/

        int vertexShaderId = ShaderUtils.createShader(context, GL_VERTEX_SHADER, R.raw.vertex_shader);
        int fragmentShaderId = ShaderUtils.createShader(context, GL_FRAGMENT_SHADER, R.raw.super_fragment_shader);
        programId = ShaderUtils.createProgram(vertexShaderId, fragmentShaderId);
        //glUseProgram(programId);
    }


    private void createProjectionMatrix(int width, int height) {
        Log.d("WhAt HaPpEnS: ", "pRoJeCtIoNs");

        float left = -1;
        float right = 1;
        float bottom = -1;
        float top = 1;
        float near = -12;
        float far = 12;

        float ratio;

        if (width > height) {
            ratio = (float) width / height;
            left *= ratio;
            right *= ratio;
        } else {
            ratio = (float) height / width;
            bottom *= ratio;
            top *= ratio;
        }

        Matrix.orthoM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
        //Matrix.orthoM(mProjectionMatrix, 0, -width/2.0f, width/2.0f, -height/2.0f, height/2.0f, -1, 1);
        //Matrix.orthoM(mProjectionMatrix, 0, 0, width, 0, height, -1, 1);
    }


    private void createViewMatrix(int w, int h) {
        float eyeX = 0;
        float eyeY = 0;
        float eyeZ = 7;

        float centerX = 0;
        float centerY = 0;
        float centerZ = 0;

        float upX = 0;
        float upY = 1;
        float upZ = 0;

        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
        Log.d("VIEW MATRIX: ", logMatrix(mViewMatrix));
    }

    private void createModelMatrix(int wi, int hi, int ws, int hs) {
        float scaling = 1;
        float wideI = (float) wi / hi;
        float wideS = (float) ws / hs;

        Matrix.setIdentityM(mModelMatrix, 0);

        if (wideI > wideS) {
            scaling *= (float) hi / wi;
            Log.d("MODEL MATRIX SCALING: ", "" + scaling);
            Matrix.scaleM(mModelMatrix, 0, 1, scaling, 1);
        } else {
            scaling *= (float) wi / hi;
            Log.d("MODEL MATRIX SCALING: ", "" + scaling);
            Matrix.scaleM(mModelMatrix, 0, scaling, 1, 1);
        }
    }

    private void bindMatrix() {
        glUniformMatrix4fv(uProjMatrixLocation, 1, false, mProjectionMatrix, 0);
        glUniformMatrix4fv(uModelMatrixLocation, 1, false, mModelMatrix, 0);
        glUniformMatrix4fv(uViewMatrixLocation, 1, false, mViewMatrix, 0);
    }

    private String logMatrix(float[] m) {
        String str = "";
        for (int i = 0; i < m.length; i++) {
            str += m[i] + ", ";
        }
        return str;
    }
}
