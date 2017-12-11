package com.example.paskevich.sixhandstestapp1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_COLOR_ATTACHMENT0;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_FRAMEBUFFER_BINDING;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_LINES;
import static android.opengl.GLES20.GL_LINE_LOOP;
import static android.opengl.GLES20.GL_LINE_STRIP;
import static android.opengl.GLES20.GL_RENDERBUFFER;
import static android.opengl.GLES20.GL_RENDERBUFFER_BINDING;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_BINDING_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindRenderbuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glFramebufferTexture2D;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetIntegerv;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glLineWidth;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;

/**
 * Created by paskevich on 01.12.17.
 */

public class FboRenderer {

    private int programId;
    private int oldFbId;
    private int fboId;
    private int oldRbID;
    private int oldTexId;
    private int fboTex;


    private float[] mProjectionMatrix = new float[16];
    private float[] mModelMatrix = new float[16];

    private final float TOUCH_SCALE_FACTOR = 180.0f/270;
    private final int TRIANGLE = 30;
    private final int SQUARE = 40;
    private final int NOTHING = 0;
    private int choosenObject;

    public FboRenderer(Context context){
        this.context = context;
    }

    Context context;

    private Bitmap bitmapFBO;

    private FloatBuffer squareVertData;
    private FloatBuffer triangleVertData;
    private FloatBuffer texVertData;

    private int texFbo = -1;

    private int aPositionLocation;
    //private int aTextureLocation;
    //private int uTextureUnitLocation;
    private int uProjMatrixLocation;
    private int uModelMatrixLocation;
    //private int uViewMatrixLocation;
    private int uColorLocation;

    private final float[] squareVert = {
            0f, 300f, 1,
            0f, 0f, 1,
            300f, 300f, 1,
            300f, 0f, 1,
    };

    private final float[] triangleVert = {
            500f, 800f, 1,
            800f, 800f, 1,
            650f, 540f, 1,
    };

    private float[] cloneSquareVert;
    private float[] cloneTriangleVert;

    protected void fboInit(int ws, int hs) {

        Log.d("tyui", "fboInit: ");

        int[] tmp = new int[1];

        //curent FB
        glGetIntegerv(GL_FRAMEBUFFER_BINDING, tmp, 0);
        oldFbId = tmp[0];

        //current RB
        glGetIntegerv(GL_RENDERBUFFER_BINDING, tmp, 0);
        oldRbID = tmp[0];

        glGetIntegerv(GL_TEXTURE_BINDING_2D, tmp, 0);
        oldTexId = tmp[0];

        //generate fbo id
        glGenFramebuffers(1, tmp, 0);
        fboId = tmp[0];

        //generate texture
        glGenTextures(1, tmp, 0);
        fboTex = tmp[0];

        //Bind FB
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);

        //Bind texture
        glBindTexture(GL_TEXTURE_2D, fboTex);

        //Define texture parameters
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, ws, hs, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, fboTex, 0);

        createProgram();
        glUseProgram(programId);
        cloneSquareVert = squareVert.clone();
        cloneTriangleVert = triangleVert.clone();
        prepFBO();
        getLocations();

        //changing to old
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindRenderbuffer(GL_RENDERBUFFER, oldRbID);
        glBindFramebuffer(GL_FRAMEBUFFER, oldFbId);
    }

    protected void prepFBO() {
      /*  float[] texVert = {
                0, 0,
                1, 0,
                0, 1,
                1, 1,
        };*/

        Log.d("tyui", "prepFBO: ");

        squareVertData = ByteBuffer
                .allocateDirect(cloneSquareVert.length*4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        squareVertData.put(cloneSquareVert);

        triangleVertData = ByteBuffer
                .allocateDirect(cloneTriangleVert.length*4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        triangleVertData.put(cloneTriangleVert);

        /*texVertData = ByteBuffer
                .allocateDirect(texVert.length*4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        texVertData.put(texVert);*/

        /*if (texFbo != -1) {
            glDeleteTextures(1, IntBuffer.allocate(texFbo));
        }*/
        texFbo = TextureUtils.loadTexture
                (context, BitmapFactory.decodeResource(context.getResources(), R.drawable.innah));
    }

    protected void getLocations() {
        aPositionLocation = glGetAttribLocation(this.programId, "a_Position");
        //aTextureLocation = glGetAttribLocation(programId, "a_Texture");
        uProjMatrixLocation = glGetUniformLocation(programId, "u_ProjMatrix");
        uModelMatrixLocation = glGetUniformLocation(programId, "u_ModelMatrix");
        //uViewMatrixLocation = glGetUniformLocation(programId, "u_ViewMatrix");
        //uTextureUnitLocation = glGetUniformLocation(programId, "u_TextureUnit");
        uColorLocation = glGetUniformLocation(this.programId, "u_Color");
    }

    protected void bindFboData(FloatBuffer objectData) {
        objectData.position(0);
        glVertexAttribPointer(aPositionLocation, 3, GL_FLOAT, false, 0, objectData);
        glEnableVertexAttribArray(aPositionLocation);

        glUniform4f(uColorLocation, 1.0f, 0.0f, 0.0f, 1.0f);



       /* texVertData.position(0);
        glVertexAttribPointer(aTextureLocation, 2,GL_FLOAT, false, 0, texVertData);
        glEnableVertexAttribArray(aTextureLocation);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texFbo);
        glUniform1i(uTextureUnitLocation, 0);*/
    }

    protected void createProgram() {
        int vertexShaderId = ShaderUtils.createShader(context, GL_VERTEX_SHADER, R.raw.vertex_shader_fbo);
        int fragmentShaderId = ShaderUtils.createShader(context, GL_FRAGMENT_SHADER, R.raw.fragment_shader_fbo);
        programId = ShaderUtils.createProgram(vertexShaderId, fragmentShaderId);
        //glUseProgram(programId);
    }

    protected void createProjectionMatrix(int width, int height) {
        Matrix.orthoM(mProjectionMatrix, 0, 0, width, 0, height, -12, 12);
    }

    private void createModelMatrix(int wi, int hi, int ws, int hs) {
        float scaling = 1;
        float wideI = (float) wi / hi;
        float wideS = (float) ws / hs;

        Matrix.setIdentityM(mModelMatrix, 0);

        if (wideI > wideS) {
            scaling *= wideS/wideI;
            Log.d("MODEL MATRIX SCALING: ", "" + scaling);
            //Matrix.translateM(mModelMatrix, 0, 0, (hs/2.0f)*(1 - scaling), 0);
            Matrix.scaleM(mModelMatrix, 0, 1, scaling, 1);
        } else {
            scaling *= (float) wideI / wideS;
            Log.d("MODEL MATRIX SCALING: ", "" + scaling);
            //Matrix.translateM(mModelMatrix, 0, (ws/2.0f)*(1 - scaling), 0, 0);
            Matrix.scaleM(mModelMatrix, 0, scaling, 1, 1);
        }
    }

    private void bindMatrix() {
        glUniformMatrix4fv(uProjMatrixLocation, 1, false, mProjectionMatrix, 0);
        glUniformMatrix4fv(uModelMatrixLocation, 1, false, mModelMatrix, 0);
    }

    public int getOldFbId() {
        return this.oldFbId;
    }

    public int getFboId() {
        return this.fboId;
    }

    public int getOldRb() {
        return this.oldRbID;
    }

    public int getOldTexId() {
        return oldTexId;
    }

    public int getFboTex() {
        return this.fboTex;
    }

    public int getProgramId() {
        return this.programId;
    }

    public boolean moveObject(float x, float y, float prevX, float prevY) {

        if(choosenObject == NOTHING) {
            return false;
        }

        float dx = x - prevX;
        float dy = y - prevY;

        if (choosenObject == SQUARE) {
            for (int i = 0; i < cloneSquareVert.length; i += 3) {
                cloneSquareVert[i] += dx * TOUCH_SCALE_FACTOR;
                cloneSquareVert[i + 1] += dy * TOUCH_SCALE_FACTOR;
            }
            squareVertData = ByteBuffer
                    .allocateDirect(cloneSquareVert.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            squareVertData.put(cloneSquareVert);
        }

        if (choosenObject == TRIANGLE) {
            for (int i = 0; i < cloneTriangleVert.length; i += 3) {
                cloneTriangleVert[i] += dx * TOUCH_SCALE_FACTOR;
                cloneTriangleVert[i + 1] += dy * TOUCH_SCALE_FACTOR;
            }
            triangleVertData = ByteBuffer
                    .allocateDirect(cloneTriangleVert.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            triangleVertData.put(cloneTriangleVert);
        }

        return true;
    }

    protected void drawObjects(int ws, int hs, int wi, int hi) {
        glUseProgram(programId);
        createProjectionMatrix(ws, hs);
        createModelMatrix(ws, hs, wi, hi);
        bindFboData(squareVertData);
        bindMatrix();

        // TODO: 11.12.17 границы фигур черным
        glLineWidth(6);

        glClear(GL_COLOR_BUFFER_BIT);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glUniform4f(uColorLocation, 0, 1, 0, 1);
        glDrawArrays(GL_LINE_LOOP , 0, 4);

        bindFboData(triangleVertData);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glUniform4f(uColorLocation, 0, 1, 0, 1);
        glDrawArrays(GL_LINE_LOOP, 0, 3);
    }

    public void setObject(float x, float y) {

        if (x > cloneSquareVert[0]
                && x < cloneSquareVert[6]
                && y > cloneSquareVert[4]
                && y < cloneSquareVert[7]) {

            choosenObject = SQUARE;

        } else {
            if (x > cloneTriangleVert[0]
                    && x < cloneTriangleVert[3]
                    && y > cloneTriangleVert[7]
                    && y < cloneTriangleVert[4]) {

                choosenObject = TRIANGLE;
            } else {
                choosenObject = NOTHING;
            }
        }
    }
}
