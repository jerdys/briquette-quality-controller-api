package ru.kpfu.itis.diploma.backend.service.analyze;

import ru.kpfu.itis.diploma.backend.model.BriquetteSide;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@Builder
@RequiredArgsConstructor
public class AlgorithmParameters {
    public static final AlgorithmParameters TEST_A4_PAPER = new AlgorithmParameters(
            new BriquetteSize(297, 210, 200),
            new StitchParameters(5, 0.1, 40, 1000)
    );

    public final BriquetteSize briquetteSize;
    public final StitchParameters stitch;

    /**
     * <pre>
     *           __________________________ __
     *         /                         /|   ^
     *       /                         /  |   | height, millimeters
     *     /________________________ /    | __v
     *    |                         |    /   ^
     *    |                         |  /   / depth, millimeters
     *    |_________________________|/ __v
     *    |                         |
     *     <----------------------->
     *        length, millimeters
     * </pre>
     */
    @Builder
    @RequiredArgsConstructor
    public static class BriquetteSize {

        /**
         * <pre>
         *           __________________________
         *         /                         /|
         *       /                         /  |
         *     /________________________ /    |
         *    |                         |    /
         *    |                         |  /
         *    |_________________________|/
         *    |                         |
         *     <b><-----------------------></b>
         *        <b>length, millimeters</b>
         * </pre>
         */
        public final int length;

        /**
         * <pre>
         *           __________________________
         *         /                         /|
         *       /                         /  |
         *     /________________________ /    |
         *    |                         |    /   <b>^</b>
         *    |                         |  /   <b>/ depth, millimeters</b>
         *    |_________________________|/ __<b>v</b>
         * </pre>
         */
        public final int depth;

        /**
         * <pre>
         *           __________________________ <b>__</b>
         *         /                         /| <b>  ^</b>
         *       /                         /  | <b>  | height, millimeters</b>
         *     /________________________ /    | <b>__v</b>
         *    |                         |    /
         *    |                         |  /
         *    |_________________________|/
         * </pre>
         */
        public final int height;
    }

    @Builder
    @RequiredArgsConstructor
    public static class StitchParameters {
        /**
         * minimum collected frames for start stitching
         */
        public final int minFrames;
        /**
         * Distance to the boundary beyond which
         * the corner of the briquette must be located
         * to recognize its third edge.
         * <p>
         * e.g., for x axis:
         * <pre>
         *     [0]               [image_width]
         *      v                      v
         *      | n |   m          |  h|
         *          ^              ^
         *         [a]            [b]
         *
         *       a = minDistanceFromBorderAbs * image_width;
         *       b = (1 - minDistanceFromBorderAbs) * image_width;
         *
         *       points <i>n</i> and <i>h</i> will not be considered as corners of the briquette,
         *       but point <i>m</i> will be considered  corner of the briquette
         * </pre>
         */
        public final double minDistanceFromBorderAbs;
        public final int maxEmptyFrames;
        public final long maxAnalyzePauseMillis;
    }

    @RequiredArgsConstructor
    public static class Directions {
        public final double top;
        public final double bottom;
        public final double left;
        public final double right;
        public final double front;
        public final double back;

        public double get(BriquetteSide side) {
            // TODO: 27.12.2019 rewrite
            switch (side) {
                case TOP:
                    return top;
                case BOTTOM:
                    return bottom;
                case LEFT:
                    return left;
                case RIGHT:
                    return right;
                case FRONT:
                    return front;
                default:
                    return back;
            }
        }
    }
}
