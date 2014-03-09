package ch.squash.simulation.shapes.common;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import ch.squash.simulation.common.Settings;
import ch.squash.simulation.main.SquashRenderer;
import ch.squash.simulation.shapes.shapes.DummyShape;

public abstract class AbstractShape {
	// constant
	private final static String TAG = AbstractShape.class.getSimpleName();
	private static final int BYTES_PER_FLOAT = 4;
	private static final int POSITION_DATA_SIZE = 3;
	private static final int COLOR_DATA_SIZE = 4;

	// data - drawing
	protected FloatBuffer mPositions;
	protected FloatBuffer mColors;
	private int mDrawMode;
	private int mVertexCount;
	private boolean mVisible = true;
	private boolean isInitialized;
	
	// data - shape
	public final String tag;
	protected final IVector origin;
	protected final IVector location;
	private SolidType mSolidType;
	private Movable mMovable;
	public float temperature = 20;

	// matrices
	private float[] mModelMatrix = new float[16];
	private float[] mMVPMatrix = new float[16];

	public AbstractShape(final String tag, final float x, final float y, final float z, final float[] mVertexData, final float[] color) {
		mVertexCount = mVertexData.length / POSITION_DATA_SIZE;
		final float[] mColorData = getColorData(color);

		this.tag = tag;
		location = new Vector(x, y, z);
		origin = new Vector(x, y, z);
		
		// Initialize the buffers.
		mPositions = ByteBuffer
				.allocateDirect(mVertexData.length * BYTES_PER_FLOAT)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mPositions.put(mVertexData).position(0);

		mColors = ByteBuffer.allocateDirect(mColorData.length * BYTES_PER_FLOAT)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mColors.put(mColorData).position(0);
	}
	
	@SuppressWarnings("unused")
	private AbstractShape() {
		location = null;
		origin = null;
		tag = null;
	}

	public void initialize(final int drawMode, final SolidType type, final Movable movable) {
		mDrawMode = drawMode;
		mSolidType = type;
		mMovable = movable;

		if (movable != null){
			for (final PhysicalVector v : mMovable.vectorArrows) {
				v.moveTo(location);
			}
		}
		
		isInitialized = true;
	}

	protected abstract float[] getColorData(final float[] color);

	public void setVisible(final boolean visible) {
		mVisible = visible;
	}
	
	public Movable getMovable() {
		return mMovable;
	}

	public boolean isSolid() {
		return mSolidType != null;
	}

	public SolidType getSolidType(){
		return mSolidType;
	}
	
	public boolean isMovable() {
		return mMovable != null;
	}
	
	public void setNewVertices(final float[] positionData) {
		mVertexCount = positionData.length / POSITION_DATA_SIZE;
		mPositions = ByteBuffer
				.allocateDirect(positionData.length * BYTES_PER_FLOAT)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mPositions.put(positionData).position(0);
	}
	
	public void draw() {
		if (!isInitialized) {
			Log.e(TAG, "Drawing uninitialized shape: " + toString());
			return;
		}
		
		if (!mVisible || mVertexCount == 0 || this instanceof DummyShape)
			return;

		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, location.getX(), location.getY(), location.getZ());

		if (isMovable()){
			if (Settings.isDrawForces())
				for (final PhysicalVector v : mMovable.vectorArrows)
					v.draw();
			
			mMovable.trace.draw();
		}

		// Pass in the position information
		mPositions.position(0);
		GLES20.glVertexAttribPointer(
				SquashRenderer.getInstance().mPositionHandle,
				POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mPositions);

		GLES20.glEnableVertexAttribArray(SquashRenderer.getInstance().mPositionHandle);

		// Pass in the color information
		mColors.position(0);
		GLES20.glVertexAttribPointer(SquashRenderer.getInstance().mColorHandle,
				COLOR_DATA_SIZE, GLES20.GL_FLOAT, false, 0, mColors);

		GLES20.glEnableVertexAttribArray(SquashRenderer.getInstance().mColorHandle);

		// (which currently contains model * view).
		Matrix.multiplyMM(mMVPMatrix, 0,
				SquashRenderer.getInstance().mViewMatrix, 0, mModelMatrix, 0);

		// Pass in the modelview matrix.
		GLES20.glUniformMatrix4fv(SquashRenderer.getInstance().mMVMatrixHandle,
				1, false, mMVPMatrix, 0);

		// (which now contains model * view * projection).
		Matrix.multiplyMM(mMVPMatrix, 0,
				SquashRenderer.getInstance().mProjectionMatrix, 0, mMVPMatrix,
				0);

		// Pass in the combined matrix.
		GLES20.glUniformMatrix4fv(
				SquashRenderer.getInstance().mMVPMatrixHandle, 1, false,
				mMVPMatrix, 0);

		// Draw the cube.
		final int drawMode = Settings.getDrawMode();
		GLES20.glDrawArrays(drawMode == -1 ? mDrawMode : drawMode, 0,
				mVertexCount);
	}

	public void move(final IVector dv) {
		moveTo(location.add(dv));
	}

	public void moveTo(final IVector dv) {
		location.setDirection(dv.getX(), dv.getY(), dv.getZ());

		if (isMovable()){
			for (final PhysicalVector gs : mMovable.vectorArrows)
				gs.moveTo(dv);
			mMovable.trace.addPoint(dv);
		}
	}

	public static boolean areEqual(final float a, final float b) {
		return areEqual(a, b, 10 * Float.MIN_NORMAL);
	}
	
	public static boolean areEqual(final float a, final float b, final float epsilon) {
		final float absA = Math.abs(a);
		final float absB = Math.abs(b);
		final float diff = Math.abs(a - b);

		boolean result;
		
		if (a == b) { // shortcut, handles infinities
			result = true;
		} else if (a == 0 || b == 0 || diff < Float.MIN_NORMAL) {
			// a or b is zero or both are extremely close to it
			// relative error is less meaningful here
			result = diff < (epsilon * Float.MIN_NORMAL);
		} else { // use relative error
			result = diff / (absA + absB) < epsilon;
		}
		
		return result;
	}

	public static float getPointPointDistance(final float[] p1, final float[] p2) {
		double result = 0;
		
		if (p1.length == p2.length){
			for (int i = 0; i < p1.length; i++)
				result += Math.pow(p1[i] - p2[i], 2);
			
			result = Math.sqrt(result);
		}else{
			Log.e(TAG, "Both points must have the same amount of dimensions");
			result = -1;
		}
		
		return (float) result;
	}
	
	public IVector getLocation(){
		return location;
	}
}