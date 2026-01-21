import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HttpEventType } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { CsvUploadRecord } from '../models/upload-record';
import { UploadService } from './upload.service';

describe('UploadService', () => {
  let service: UploadService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(UploadService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('fetches the upload history', () => {
    const mockUploads: CsvUploadRecord[] = [
      {
        id: 1,
        filename: 'demo.csv',
        status: 'PENDING',
        s3Key: 'key',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z'
      }
    ];

    let result: CsvUploadRecord[] | null = null;

    service.listUploads().subscribe(records => (result = records));

    const req = httpMock.expectOne('http://localhost:8080/api/uploads');
    expect(req.request.method).toBe('GET');
    req.flush(mockUploads);

    expect(result).toEqual(mockUploads);
  });

  it('emits progress updates while uploading', () => {
    const events: any[] = [];
    const file = new File(['id'], 'demo.csv', { type: 'text/csv' });
    const response: CsvUploadRecord = {
      id: 5,
      filename: 'demo.csv',
      status: 'PENDING',
      s3Key: 'key',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z'
    };

    service.uploadFile(file).subscribe(event => events.push(event));

    const req = httpMock.expectOne('http://localhost:8080/api/uploads');
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBe(true);

    req.event({ type: HttpEventType.UploadProgress, loaded: 50, total: 100 });
    req.flush(response);

    expect(events).toEqual([
      { kind: 'progress', progress: 50 },
      { kind: 'success', upload: response }
    ]);
  });
});
