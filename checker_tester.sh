rm results.txt

for i in {1..20}
do
    OUTPUT="$(./checkers waffle.sh "java ABHelper" 3 | tail -20)"
    echo $OUTPUT | grep -q 'Player 2 has lost the game' && echo 'Win' >> results.txt
    echo $OUTPUT | grep -q 'Player 1 has lost the game' && echo 'Lost' >> results.txt
    echo $OUTPUT | grep -q 'Draw' && echo 'Draw' >> results.txt

    killall -9 java
done
