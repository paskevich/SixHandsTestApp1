package com.example.paskevich.sixhandstestapp1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.support.annotation.RestrictTo;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glDrawElements;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glGenRenderbuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetFramebufferAttachmentParameteriv;
import static android.opengl.GLES20.glGetIntegerv;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;
import static javax.microedition.khronos.opengles.GL10.GL_TEXTURE1;

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

    private int currProgramId;

    private int maskModeProgramId;
    private int allModeProgramId;

    private float[] mProjectionMatrix = new float[16];
    private float[] mModelMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private int height;
    private int width;

    private boolean wasBitmapChanged;
    private float scaling;
    private float wideI;
    private float wideS;

    public MyRenderer(Context context) {
        this.context = context;

        fboRenderer = new FboRenderer(context);
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        fboBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.innah);
        glClearColor(0f, 0f, 0f, 1f);
        Log.d("Surface: ", "surface created");
        glEnable(GL_DEPTH_TEST);

        createProgram();
        currProgramId = maskModeProgramId;
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        Log.d("Surface", "onSurfaceChanged: width: " + width + " height: " + height);
        //glViewport(0, 0, width, height);

        this.width = width;
        this.height = height;

        glViewport(0, 0, width, height);

        if (bitmapImage != null) {
            //Log.d("QWERTY", "onSurfaceChanged: Image Width: " + bitmapImage.getWidth() + " Image Height: " + bitmapImage.getHeight());
            if (wasBitmapChanged) {
                fboRenderer.fboInit(width, height);
            }
            glBindFramebuffer(GL_FRAMEBUFFER, fboRenderer.getOldFbId());
            prepareData(width, height);
        }
    }


    @Override
    public void onDrawFrame(GL10 gl10) {

        Log.d("Surface", "onDrawFrame: ");

        if (bitmapImage != null) {
            glBindFramebuffer(GL_FRAMEBUFFER, fboRenderer.getFboId());
            fboRenderer.drawObjects(width, height, bitmapImage.getWidth(), bitmapImage.getHeight());

            glBindFramebuffer(GL_FRAMEBUFFER, fboRenderer.getOldFbId());
            glUseProgram(currProgramId);
            drawScene();

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        } else {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        }
    }

    public void setBitmapImage(Bitmap bitmapImage) {
        if (this.bitmapImage == null) {
            this.bitmapImage = bitmapImage;
            wasBitmapChanged = true;
        } else {
            if (this.bitmapImage.equals(bitmapImage)) {
                wasBitmapChanged = false;
            } else {
                this.bitmapImage = bitmapImage;
                wasBitmapChanged = true;
            }
        }
    }

    private void prepareData(int w, int h) {

        float[] vertices = {
                0, h, 1, 0, 0,
                w, h, 1, 1, 0,
                0, 0, 1, 0, 1,
                w, 0, 1, 1, 1,
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
        aPositionLocation = glGetAttribLocation(currProgramId, "a_Position");
        aTextureLocation = glGetAttribLocation(currProgramId, "a_Texture");
        uProjMatrixLocation = glGetUniformLocation(currProgramId, "u_ProjMatrix");
        uModelMatrixLocation = glGetUniformLocation(currProgramId, "u_ModelMatrix");
        uViewMatrixLocation = glGetUniformLocation(currProgramId, "u_ViewMatrix");

        //uncomment for another shaders
        //uTextureUnitLocation = glGetUniformLocation(currProgramId, "u_TextureUnit");

        uImageTexLocation = glGetUniformLocation(currProgramId, "u_ImageTex");
        uMaskTexLocation = glGetUniformLocation(currProgramId, "u_MaskTex");
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
       /* int vertexShaderId = ShaderUtils.createShader(context, GL_VERTEX_SHADER, R.raw.vertex_shader);
        int fragmentShaderId = ShaderUtils.createShader(context, GL_FRAGMENT_SHADER, R.raw.fragment_shader);*/

        int vertexShaderId = ShaderUtils.createShader(context, GL_VERTEX_SHADER, R.raw.vertex_shader);
        int fragmentShaderId = ShaderUtils.createShader(context, GL_FRAGMENT_SHADER, R.raw.super_fragment_shader);
        maskModeProgramId = ShaderUtils.createProgram(vertexShaderId, fragmentShaderId);
        //glUseProgram(currProgramId);

        fragmentShaderId = ShaderUtils.createShader(context, GL_FRAGMENT_SHADER, R.raw.super_fragment_shader_2);
        allModeProgramId = ShaderUtils.createProgram(vertexShaderId, fragmentShaderId);
    }


    private void createProjectionMatrix(int width, int height) {
        Log.d("WhAt HaPpEnS: ", "pRoJeCtIoNs");

        Matrix.orthoM(mProjectionMatrix, 0, 0, width, 0, height, -12, 12);
    }

    private void createModelMatrix(int wi, int hi, int ws, int hs) {
        scaling = 1;
        wideI = (float) wi / hi;
        wideS = (float) ws / hs;

        Matrix.setIdentityM(mModelMatrix, 0);

        if (wideI > wideS) {
            scaling *= wideS/wideI;
            Log.d("MODEL MATRIX SCALING: ", "" + scaling);
            Matrix.translateM(mModelMatrix, 0, 0, (hs/2.0f)*(1 - scaling), 0);
            Matrix.scaleM(mModelMatrix, 0, 1, scaling, 1);
        } else {
            scaling *= wideI / wideS;
            Log.d("MODEL MATRIX SCALING: ", "" + scaling);
            Matrix.translateM(mModelMatrix, 0, (ws/2.0f)*(1 - scaling), 0, 0);
            Matrix.scaleM(mModelMatrix, 0, scaling, 1, 1);
        }
    }

    private void bindMatrix() {
        glUniformMatrix4fv(uProjMatrixLocation, 1, false, mProjectionMatrix, 0);
        glUniformMatrix4fv(uModelMatrixLocation, 1, false, mModelMatrix, 0);
    }

    private String logMatrix(float[] m) {
        String str = "";
        for (int i = 0; i < m.length; i++) {
            str += m[i] + ", ";
        }
        return str;
    }

    public void setCurrProgramId(boolean isMaskMode) {
        if (isMaskMode == true){
            this.currProgramId = maskModeProgramId;
        } else {
            this.currProgramId = allModeProgramId;
        }
    }

    public FboRenderer getFboRenderer() {
        return this.fboRenderer;
    }

    private void drawScene() {
        getLocations();
        bindData();
        createProjectionMatrix(width, height);
        createModelMatrix(bitmapImage.getWidth(), bitmapImage.getHeight(), width, height);
        //createViewMatrix(width, height);
        bindMatrix();
    }

    protected Bitmap getBitmapImage() {
        return this.bitmapImage;
    }

    protected float getScaling() {
        return this.scaling;
    }

    protected float getWideI() {
        return this.wideI;
    }

    protected float getWideS() {
        return this.wideS;
    }

    protected int getWidth() {
        return this.width;
    }

    protected int getHeight() {
        return this.height;
    }

    protected float[] getmModelMatrix() {
        return this.mModelMatrix;
    }
}