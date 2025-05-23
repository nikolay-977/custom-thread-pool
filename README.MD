## Установка и запуск

### Для запуска проекта выполните следующие шаги:

1. Клонируйте репозиторий:

```bash
git clone https://github.com/nikolay-977/custom-thread-pool
cd custom-thread-pool
 ```

2. Скомпилируйте проект:

```bash
mvn compile
 ```

3. Запустите:

```bash
mvn exec:java
 ```

## Анализ производительности
   
В ходе тестирования производительности кастомного пула потоков (CustomThreadPool) 
и стандартного пула (ThreadPoolExecutor) были получены следующие результаты: [performance_results.csv](performance_results.csv)

### Выводы:

Стандартный пул (ThreadPoolExecutor) показывает более высокую производительность по сравнению с кастомным пулом в большинстве конфигураций.
Кастомный пул имеет более высокие значения времени выполнения, что может указывать на дополнительные накладные расходы, связанные с его реализацией.

## Мини-исследование параметров пула

В ходе мини-исследования были протестированы различные значения параметров пула, 
чтобы определить, какие из них приводят к максимальным показателям производительности. 
Результаты исследования: [research_results.csv](research_results.csv)

### Выводы:

Увеличение queueSize и minSpareThreads в большинстве случаев приводит к улучшению производительности.
Оптимальные значения параметров варьируются, но в целом, 
увеличение minSpareThreads и queueSize способствует более эффективной обработке задач.

## Принцип действия механизма распределения задач

Кастомный пул потоков использует несколько внутренних очередей задач — по одной на каждый базовый (core) поток.

При поступлении задачи выполняется такая логика:

Сначала пытается добавить задачу в случайно выбранную очередь.
Если не удалось, пытается добавить в наименее загруженную очередь (с наименьшим количеством задач).
Если все очереди заполнены, и количество потоков меньше максимума, создаётся новый поток для обработки задачи.
Если и это невозможно — задача отклоняется через заданный обработчик отклонённых задач.
Такой подход обеспечивает балансировку нагрузки между потоками, 
снижая вероятность "горячих точек" (очередей с большим числом задач) и помогает эффективно использовать ресурсы пула. 
Также параметр minSpareThreads задаёт минимальное число резервных потоков, которые поддерживаются активными, 
чтобы пул мог быстро реагировать на всплески нагрузки.