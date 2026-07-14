export interface KafkaMetrics {
  status: "HEALTHY" | "DEGRADED" | "UNHEALTHY";
  consumerLag: number;
  publishRate: number; // events/sec
  consumptionRate: number; // events/sec
  failedPublishesCount: number;
}

export const KafkaMonitoringService = {
  async getMetrics(): Promise<KafkaMetrics> {
    return {
      status: "HEALTHY",
      consumerLag: 8,
      publishRate: 142,
      consumptionRate: 140,
      failedPublishesCount: 0,
    };
  }
};
