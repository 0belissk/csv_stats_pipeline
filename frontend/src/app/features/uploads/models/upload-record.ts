export type CsvUploadStatus = 'PENDING' | 'VALIDATING' | 'VALIDATED' | 'VALIDATION_FAILED';

export interface CsvUploadRecord {
  id: number;
  filename: string;
  status: CsvUploadStatus;
  s3Key: string;
  createdAt: string;
  updatedAt: string;
}
