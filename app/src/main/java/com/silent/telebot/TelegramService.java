private void startPoller() {
    pollerThread = new Thread(() -> {
        while (running) {
            try {
                new TelegramPoller(TelegramService.this).run();
                Thread.sleep(5000); // ← 5 ثوانٍ للاستجابة السريعة
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    });
    pollerThread.start();
}
