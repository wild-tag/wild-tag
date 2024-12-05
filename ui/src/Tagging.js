import React, { useEffect, useState, useRef } from 'react';
import { Stage, Layer, Rect, Text, Image as KonvaImage } from 'react-konva';
import './Tagging.css';
import apiCall from './services/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faUndo, faRedo } from '@fortawesome/free-solid-svg-icons';

function TaggingPage() {
    const [imageSrc, setImageSrc] = useState('');
    const [currentImageId, setCurrentImageId] = useState(''); // Updated to track the image ID
    const [categories, setCategories] = useState({});
    const [animalColors, setAnimalColors] = useState({});
    const [boxes, setBoxes] = useState([]);
    const [drawing, setDrawing] = useState(false);
    const [newBox, setNewBox] = useState(null);
    const [imageSize, setImageSize] = useState({ width: 0, height: 0 });
    const [canvasScale, setCanvasScale] = useState({ x: 1, y: 1 });
    const [selectedAnimal, setSelectedAnimal] = useState('');
    const [history, setHistory] = useState([[]]);
    const [redoHistory, setRedoHistory] = useState([]);
    const [isLoading, setIsLoading] = useState(false); // Loading state for the image
    const stageRef = useRef(null);
    const containerRef = useRef(null);

    const usedColors = new Set();

    const generateUniqueBrightColor = () => {
        let color;
        do {
            const r = Math.floor(Math.random() * 156 + 100);
            const g = Math.floor(Math.random() * 156 + 100);
            const b = Math.floor(Math.random() * 156 + 100);
            color = `rgb(${r}, ${g}, ${b})`;
        } while (usedColors.has(color));
        usedColors.add(color);
        return color;
    };

    useEffect(() => {
        const fetchCategories = async () => {
            try {
                const result = await apiCall('/wild-tag/categories', 'GET', null, {
                    Authorization: `Bearer ${localStorage.getItem('authToken')}`,
                });

                const categoriesData = result.entries || {};
                setCategories(categoriesData);

                const colors = {};
                Object.values(categoriesData).forEach((animal) => {
                    colors[animal] = generateUniqueBrightColor();
                });

                setAnimalColors(colors);
            } catch (err) {
                console.error('Error fetching categories:', err.message);
            }
        };

        fetchCategories();
    }, []);

    const fetchNextTask = async () => {
        setIsLoading(true); // Start loading
        try {
            const imageResult = await apiCall('/wild-tag/images/next_task', 'GET', null, {
                Authorization: `Bearer ${localStorage.getItem('authToken')}`,
            });

            if (imageResult && imageResult.id) {
                setCurrentImageId(imageResult.id); // Set the correct image ID
                const imageResponse = await apiCall(
                    `/wild-tag/images/${imageResult.id}`,
                    'GET',
                    null,
                    {
                        Authorization: `Bearer ${localStorage.getItem('authToken')}`,
                    },
                    'blob'
                );

                const blob = await imageResponse.blob();
                const url = URL.createObjectURL(blob);
                setImageSrc(url);

                const img = new window.Image();
                img.onload = () => {
                    setImageSize({ width: img.width, height: img.height });
                    setIsLoading(false); // Image loaded
                };
                img.src = url;
            } else {
                console.error('Failed to fetch image content');
                setIsLoading(false);
            }
        } catch (error) {
            console.error('Error fetching image:', error);
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchNextTask(); // Fetch the first task when the component mounts
    }, []);

    useEffect(() => {
        const updateDimensions = () => {
            if (containerRef.current && stageRef.current && imageSize.width && imageSize.height) {
                const containerWidth = containerRef.current.offsetWidth;
                const containerHeight = containerRef.current.offsetHeight;
                const scale = Math.min(
                    containerWidth / imageSize.width,
                    containerHeight / imageSize.height
                );
                setCanvasScale({ x: scale, y: scale });
            }
        };

        updateDimensions();
        window.addEventListener('resize', updateDimensions);
        return () => window.removeEventListener('resize', updateDimensions);
    }, [imageSize]);

    const clampValue = (value, min, max) => Math.max(min, Math.min(value, max));

    const handleMouseDown = (e) => {
        if (!selectedAnimal) return;

        const stage = e.target.getStage();
        const pos = stage.getPointerPosition();
        const { x: scaleX, y: scaleY } = canvasScale;

        setDrawing(true);
        setNewBox({
            x: clampValue(pos.x / scaleX, 0, imageSize.width),
            y: clampValue(pos.y / scaleY, 0, imageSize.height),
            width: 0,
            height: 0,
            animal: selectedAnimal,
            color: animalColors[selectedAnimal],
        });
    };

    const handleMouseMove = (e) => {
        if (!drawing) return;

        const stage = e.target.getStage();
        const pos = stage.getPointerPosition();
        const { x: scaleX, y: scaleY } = canvasScale;

        if (pos) {
            setNewBox((prev) => {
                if (!prev) return null;

                const clampedX = clampValue(pos.x / scaleX, 0, imageSize.width);
                const clampedY = clampValue(pos.y / scaleY, 0, imageSize.height);

                return {
                    ...prev,
                    width: clampedX - prev.x,
                    height: clampedY - prev.y,
                };
            });
        }
    };

    const handleMouseUp = () => {
        setDrawing(false);
        if (newBox) {
            const updatedBoxes = [...boxes, newBox];
            setBoxes(updatedBoxes);
            setHistory((prev) => [...prev, updatedBoxes]);
            setRedoHistory([]);
        }
        setNewBox(null);
    };

    const handleMouseLeave = () => {
        if (drawing) {
            setDrawing(false);
            setNewBox(null);
        }
    };

    const handleClear = () => {
        setBoxes([]);
        setHistory([[]]);
        setRedoHistory([]);
    };

    const handleUndo = () => {
        if (history.length > 1) {
            const newHistory = [...history];
            const previousState = newHistory[newHistory.length - 2];
            setRedoHistory([history[history.length - 1], ...redoHistory]);
            newHistory.pop();
            setBoxes(previousState);
            setHistory(newHistory);
        }
    };

    const handleRedo = () => {
        if (redoHistory.length > 0) {
            const nextState = redoHistory[0];
            setHistory((prev) => [...prev, nextState]);
            setBoxes(nextState);
            setRedoHistory(redoHistory.slice(1));
        }
    };

    const handleSubmit = async () => {
        if (boxes.length === 0) {
            alert("No boxes drawn. Please tag the image before submitting.");
            return;
        }

        const yoloData = boxes.map((box) => {
            const centerX = (box.x + box.width / 2) / imageSize.width;
            const centerY = (box.y + box.height / 2) / imageSize.height;
            const boxWidth = Math.abs(box.width) / imageSize.width;
            const boxHeight = Math.abs(box.height) / imageSize.height;
            const labelIndex = Object.keys(animalColors).indexOf(box.animal);

            return {
                animalId: labelIndex,
                xCenter: centerX.toFixed(6),
                yCenter: centerY.toFixed(6),
                width: boxWidth.toFixed(6),
                height: boxHeight.toFixed(6),
            };
        });

        try {
            await apiCall(`/wild-tag/images/tag`, 'PUT', { id: currentImageId, coordinates: yoloData }, {
                Authorization: `Bearer ${localStorage.getItem('authToken')}`,
            });

            fetchNextTask(); // Fetch the next task after successful submission
            setBoxes([]);
            setHistory([[]]);
            setRedoHistory([]);
            setSelectedAnimal('');
        } catch (error) {
            console.error("Error submitting tagging data or fetching the next task:", error);
        }
    };

    const animalCounts = Object.keys(animalColors).reduce((counts, animal) => {
        counts[animal] = boxes.filter((box) => box.animal === animal).length;
        return counts;
    }, {});

    return (
        <div className="image-tag-page">
            {isLoading && (
                <div className="loading-overlay">
                    <div className="loader"></div>
                </div>
            )}
            <div className="image-tag-content">
                <div className="image-tag-div" ref={containerRef}>
                    {imageSrc && (
                        <Stage
                            width={imageSize.width * canvasScale.x}
                            height={imageSize.height * canvasScale.y}
                            scale={canvasScale}
                            onMouseDown={handleMouseDown}
                            onMouseMove={handleMouseMove}
                            onMouseUp={handleMouseUp}
                            onMouseLeave={handleMouseLeave}
                            ref={stageRef}
                        >
                            <Layer>
                                <KonvaImage
                                    image={(() => {
                                        const img = new window.Image();
                                        img.src = imageSrc;
                                        return img;
                                    })()}
                                    width={imageSize.width}
                                    height={imageSize.height}
                                />
                                {boxes.map((box, i) => (
                                    <React.Fragment key={i}>
                                        <Rect
                                            x={box.x}
                                            y={box.y}
                                            width={box.width}
                                            height={box.height}
                                            stroke={box.color}
                                            strokeWidth={15}
                                        />
                                        <Text
                                            x={box.x + box.width / 2}
                                            y={box.y - 80}
                                            text={box.animal}
                                            fontSize={50}
                                            fill={box.color}
                                            align="center"
                                        />
                                    </React.Fragment>
                                ))}
                                {newBox && (
                                    <Rect
                                        x={newBox.x}
                                        y={newBox.y}
                                        width={newBox.width}
                                        height={newBox.height}
                                        stroke={newBox.color}
                                        strokeWidth={15}
                                    />
                                )}
                            </Layer>
                        </Stage>
                    )}
                </div>
                <div className="image-attributes">
                    <h3>Image Attributes</h3>
                    <ul>
                        {Object.entries(animalCounts).map(([animal, count]) => (
                            <li key={animal}>
                                <span
                                    className="color-indicator"
                                    style={{ backgroundColor: animalColors[animal] }}
                                ></span>
                                <span className="animal-name">{animal}</span>: {count}
                            </li>
                        ))}
                    </ul>
                    <div className="controls">
                        <select
                            value={selectedAnimal}
                            onChange={(e) => setSelectedAnimal(e.target.value)}
                            className="animal-select"
                        >
                            <option value="">Select animal ...</option>
                            {Object.values(categories).map((animal) => (
                                <option key={animal} value={animal}>
                                    {animal}
                                </option>
                            ))}
                        </select>
                        <button onClick={handleClear} className="button-clear">
                            Clear
                        </button>
                        <button onClick={handleUndo} className="button-undo">
                            <FontAwesomeIcon icon={faUndo} />
                        </button>
                        <button onClick={handleRedo} className="button-redo">
                            <FontAwesomeIcon icon={faRedo} />
                        </button>
                        <button onClick={handleSubmit} className="button-submit">
                            Submit
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default TaggingPage;
