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
import static android.opengl.GLES20.GL_RENDERBUFFER;
import static android.opengl.GLES20.GL_RENDERBUFFER_BINDING;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_BINDING_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
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

    public FboRenderer(Context context){
        this.context = context;
    }

    Context context;

    private Bitmap bitmapFBO;

    private FloatBuffer squareVertData;
    private FloatBuffer texVertData;

    private int texFbo = -1;

    private int aPositionLocation;
    //private int aTextureLocation;
    //private int uTextureUnitLocation;
    private int uProjMatrixLocation;
    private int uModelMatrixLocation;
    //private int uViewMatrixLocation;
    private int uColorLocation;

    protected void fboInit(int fboWidth, int fboHeight) {

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
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, fboWidth, fboHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, fboTex, 0);

        createProgram();
        glUseProgram(programId);
        prepFBO();
        getLocations();
        createProjectionMatrix(fboWidth, fboHeight);
        //createModelMatrix(fbo);
        bindFboData();
        bindMatrix();
        glClear(GL_COLOR_BUFFER_BIT);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        //changing to old
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindRenderbuffer(GL_RENDERBUFFER, oldRbID);
        glBindFramebuffer(GL_FRAMEBUFFER, oldFbId);
    }

    protected void prepFBO() {
        float[] squareVert = {
                -0.2f, 0.2f, 1,
                -0.2f, -0.2f, 1,
                0.2f, 0.2f, 1,
                0.2f, -0.2f, 1,
        };

        float[] texVert = {
                0, 0,
                1, 0,
                0, 1,
                1, 1,
        };

        squareVertData = ByteBuffer
                .allocateDirect(squareVert.length*4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        squareVertData.put(squareVert);

        texVertData = ByteBuffer
                .allocateDirect(texVert.length*4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        texVertData.put(texVert);
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

    protected void bindFboData() {
        squareVertData.position(0);
        glVertexAttribPointer(aPositionLocation, 3, GL_FLOAT, false, 0, squareVertData);
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
        //glUniformMatrix4fv(uModelMatrixLocation, 1, false, mModelMatrix, 0);
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
}
