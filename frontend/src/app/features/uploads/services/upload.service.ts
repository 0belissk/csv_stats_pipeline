import {
  HttpClient,
  HttpEventType,
  HttpRequest,
  HttpResponse,
  HttpUploadProgressEvent
} from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, filter, map } from 'rxjs';
import { API_BASE_URL } from '../../../core/api/api.config';
import { CsvUploadRecord } from '../models/upload-record';

export type UploadStreamEvent =
  | { kind: 'progress'; progress: number }
  | { kind: 'success'; upload: CsvUploadRecord };

@Injectable({ providedIn: 'root' })
export class UploadService {
  private readonly uploadsUrl = `${API_BASE_URL}/api/uploads`;

  constructor(private readonly http: HttpClient) {}

  listUploads(): Observable<CsvUploadRecord[]> {
    return this.http.get<CsvUploadRecord[]>(this.uploadsUrl);
  }

  uploadFile(file: File): Observable<UploadStreamEvent> {
    const formData = new FormData();
    formData.append('file', file, file.name);

    const request = new HttpRequest('POST', this.uploadsUrl, formData, {
      reportProgress: true
    });

    return this.http.request<CsvUploadRecord>(request).pipe(
      filter(
        (
          event
        ): event is HttpUploadProgressEvent | HttpResponse<CsvUploadRecord> =>
          event.type === HttpEventType.UploadProgress || event.type === HttpEventType.Response
      ),
      map(event => {
        if (event.type === HttpEventType.UploadProgress) {
          const progress = event.total ? Math.round((event.loaded / event.total) * 100) : 0;
          return { kind: 'progress', progress } satisfies UploadStreamEvent;
        }

        return {
          kind: 'success',
          upload: event.body as CsvUploadRecord
        } satisfies UploadStreamEvent;
      })
    );
  }
}
